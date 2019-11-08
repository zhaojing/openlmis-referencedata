/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org.
 */

package org.openlmis.referencedata.repository.custom.impl;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.collect.Lists;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.transform.DistinctRootEntityResultTransformer;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.domain.ProgramOrderable;
import org.openlmis.referencedata.domain.VersionIdentity;
import org.openlmis.referencedata.repository.custom.OrderableRepositoryCustom;
import org.openlmis.referencedata.util.Pagination;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public class OrderableRepositoryImpl implements OrderableRepositoryCustom {

  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(OrderableRepositoryImpl.class);

  private static final String FROM_ORDERABLES_TABLE = " FROM referencedata.orderables AS o";

  private static final String NATIVE_PROGRAM_ORDERABLE_JOIN =
      " JOIN referencedata.program_orderables AS po"
          + "  ON o.id = po.orderableId AND o.versionNumber = po.orderableVersionNumber";

  private static final String NATIVE_PROGRAM_ORDERABLE_INNER_JOIN =
      " INNER" + NATIVE_PROGRAM_ORDERABLE_JOIN;

  private static final String NATIVE_PROGRAM_JOIN =
      " JOIN referencedata.programs AS p"
          + "  ON p.id = po.programId";

  private static final String NATIVE_PROGRAM_INNER_JOIN =
      " INNER" + NATIVE_PROGRAM_JOIN;

  private static final String NATIVE_LATEST_ORDERABLE_INNER_JOIN =
      " INNER JOIN (SELECT id, MAX (versionNumber) AS versionNumber"
          + "  FROM referencedata.orderables GROUP BY id) AS latest"
          + "  ON o.id = latest.id AND o.versionNumber = latest.versionNumber";

  private static final String NATIVE_SELECT_LAST_UPDATED = "SELECT o.lastupdated "
      + FROM_ORDERABLES_TABLE + NATIVE_LATEST_ORDERABLE_INNER_JOIN;

  private static final String NATIVE_COUNT_LAST_UPDATED = "SELECT COUNT(*) "
      + FROM_ORDERABLES_TABLE + NATIVE_LATEST_ORDERABLE_INNER_JOIN;

  private static final String ORDER_BY_LAST_UPDATED_DESC_LIMIT_1 = " ORDER BY o.lastupdated"
      + " DESC LIMIT 1";

  private static final String WHERE = " WHERE ";
  private static final String AND = " AND ";
  private static final String ID = "id";
  private static final String IDENTITY = "identity";
  private static final String GMT = "GMT";
  private static final String VERSION_NUMBER = "versionNumber";
  private static final String FULL_PRODUCT_NAME = "fullProductName";

  // HQL queries are running into issues with bigger number of identities at once
  private static final Integer MAX_IDENTITIES_SIZE = 1000;

  @PersistenceContext
  private EntityManager entityManager;

  /**
   * This method is supposed to retrieve all orderables with matched parameters. Method is ignoring
   * case for orderable code and name. To find all wanted orderables by code and name we use
   * criteria query and like operator.
   *
   * @return List of orderables matching the parameters.
   */
  @Override
  public Page<Orderable> search(SearchParams searchParams, Pageable pageable) {
    Profiler profiler = new Profiler("ORDERABLE_REPOSITORY_SEARCH_BY_PARAMS");
    profiler.setLogger(XLOGGER);

    profiler.start("CALCULATE_FULL_LIST_SIZE");
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    Long total = 0L;

    List<VersionIdentity> identityList = new ArrayList<>();
    if (!isEmpty(searchParams.getIdentityPairs())) {
      identityList.addAll(convertPairToVersionIdentity(searchParams.getIdentityPairs()));
      for (List<VersionIdentity> part : ListUtils.partition(identityList, MAX_IDENTITIES_SIZE)) {
        CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
        total += prepareQuery(searchParams, countQuery, true, part, pageable)
            .getSingleResult();
      }
    } else {
      CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
      total += prepareQuery(searchParams, countQuery, true, identityList, pageable)
          .getSingleResult();
    }

    if (total < 1) {
      profiler.stop().log();
      return Pagination.getPage(Collections.emptyList(), pageable);
    }

    profiler.start("GET_VERSION_IDENTITY");
    List<VersionIdentity> identities = new ArrayList<>();
    if (!isEmpty(identityList)) {
      for (List<VersionIdentity> part : ListUtils.partition(identityList, MAX_IDENTITIES_SIZE)) {
        CriteriaQuery<VersionIdentity> query = builder.createQuery(VersionIdentity.class);
        identities.addAll(prepareQuery(searchParams, query, false, part, pageable)
            .getResultList());
      }
    } else {
      CriteriaQuery<VersionIdentity> query = builder.createQuery(VersionIdentity.class);
      identities.addAll(prepareQuery(searchParams, query, false, identityList, pageable)
          .getResultList());
    }

    profiler.start("RETRIEVE_ORDERABLES");
    List<Orderable> orderables = new ArrayList<>();
    for (List<VersionIdentity> partition : ListUtils.partition(identities, MAX_IDENTITIES_SIZE)) {
      orderables.addAll(retrieveOrderables(partition, false));
    }

    profiler.stop().log();
    return Pagination.getPage(orderables, pageable, total);
  }

  /**
   * This method is supposed to get the latest last update date from the retrieved orderables
   * based on params passed to the request.
   *
   * @return ZonedDateTime of the latest last updated orderable.
   */
  @Override
  public List<Orderable> findOrderablesWithLatestModifiedDate(SearchParams searchParams,
      Pageable pageable) {
    Profiler profiler = new Profiler("ORDERABLE_REPOSITORY_SEARCH_LATEST_DATE_BY_PARAMS");
    profiler.setLogger(XLOGGER);

    profiler.start("CALCULATE_FULL_LIST_SIZE");
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();

    CriteriaQuery<Long> countQuery = builder.createQuery(Long.class);
    Long total = prepareQuery(searchParams, countQuery, true, null, pageable)
        .getSingleResult();

    if (total < 1) {
      profiler.stop().log();
      return Collections.emptyList();
    }

    profiler.start("GET_VERSION_IDENTITY");
    List<VersionIdentity> identities = new ArrayList<>();

    CriteriaQuery<VersionIdentity> query = builder.createQuery(VersionIdentity.class);
    identities.addAll(prepareQuery(searchParams, query, false, null, pageable)
        .getResultList());

    profiler.start("GET_ORDERABLE_WITH_LATEST_LAST_UPDATE_DATE_FROM_ORDERABLES");
    List<Orderable> orderables = new ArrayList<>();
    for (List<VersionIdentity> partition : ListUtils.partition(identities, MAX_IDENTITIES_SIZE)) {
      orderables.addAll(retrieveOrderables(partition, true));
    }

    profiler.stop().log();
    return orderables;
  }

  @Override
  public ZonedDateTime findLatestModifiedDateByParams(SearchParams searchParams) {
    Profiler profiler = new Profiler("GET_ZONED_DATE_TIME_FROM_PARAMS");
    profiler.setLogger(XLOGGER);

    profiler.start("CALCULATE_FULL_LIST_SIZE_LAST_UPDATED");
    Query countNativeQuery = getLastUpdatedQuery(searchParams, true);
    int total = ((Number) countNativeQuery.getSingleResult()).intValue();

    if (total <= 0) {
      profiler.stop().log();
      return null;
    }

    profiler.start("GET_ZONED_DATE_TIME_QUERY");
    Query query = getLastUpdatedQuery(searchParams, false);
    Timestamp timestamp = (Timestamp) query.getSingleResult();
    profiler.stop().log();
    return ZonedDateTime.of(timestamp.toLocalDateTime(), ZoneId.of(GMT));
  }

  private <T> TypedQuery<T> prepareQuery(SearchParams searchParams, CriteriaQuery<T> query,
      boolean count, Collection<VersionIdentity> identities, Pageable pageable) {

    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    Root<Orderable> root = query.from(Orderable.class);
    root.alias("orderable");

    CriteriaQuery<T> newQuery;

    if (count) {
      CriteriaQuery<Long> countQuery = (CriteriaQuery<Long>) query;
      newQuery = (CriteriaQuery<T>) countQuery.select(builder.count(root));
    } else {
      CriteriaQuery<VersionIdentity> typeQuery = (CriteriaQuery<VersionIdentity>) query;
      newQuery = (CriteriaQuery<T>) typeQuery.select(root.get(IDENTITY));
    }

    Predicate where = builder.conjunction();

    if (null != searchParams) {
      if (null != searchParams.getProgramCode()) {
        Join<Orderable, ProgramOrderable> poJoin = root.join("programOrderables", JoinType.INNER);
        Join<ProgramOrderable, Program> programJoin = poJoin.join("program", JoinType.INNER);
        where = builder.and(where, builder.equal(builder.lower(programJoin.get("code").get("code")),
            searchParams.getProgramCode().toLowerCase()));
      }

      if (isEmpty(identities)) {
        Subquery<String> latestOrderablesQuery = createSubQuery(newQuery, builder);
        where = builder.and(where, builder.in(builder.concat(
            root.get(IDENTITY).get(ID).as(String.class),
            root.get(IDENTITY).get(VERSION_NUMBER)).as(String.class))
            .value(latestOrderablesQuery));
      } else {
        where = builder.and(where, builder.in(root.get(IDENTITY)).value(identities));
      }

      if (isNotBlank(searchParams.getCode())) {
        where = builder.and(where, builder.like(builder.lower(root.get("productCode").get("code")),
            "%" + searchParams.getCode().toLowerCase() + "%"));
      }

      if (isNotBlank(searchParams.getName())) {
        where = builder.and(where, builder.like(builder.lower(root.get(FULL_PRODUCT_NAME)),
            "%" + searchParams.getName().toLowerCase() + "%"));
      }
    } else {
      Subquery<String> latestOrderablesQuery = createSubQuery(newQuery, builder);
      where = builder.and(where, builder.in(builder.concat(
          root.get(IDENTITY).get(ID).as(String.class),
          root.get(IDENTITY).get(VERSION_NUMBER)).as(String.class))
          .value(latestOrderablesQuery));
    }

    newQuery.where(where);

    if (!count) {
      newQuery.groupBy(
          root.get(IDENTITY).get(ID),
          root.get(IDENTITY).get(VERSION_NUMBER),
          root.get(FULL_PRODUCT_NAME));
      newQuery.orderBy(builder.asc(root.get(FULL_PRODUCT_NAME)));

      return entityManager.createQuery(query)
          .setMaxResults(pageable.getPageSize())
          .setFirstResult(pageable.getOffset());
    }

    return entityManager.createQuery(newQuery);
  }

  private Subquery<String> createSubQuery(CriteriaQuery query, CriteriaBuilder builder) {
    Subquery<String> latestOrderablesQuery = query.subquery(String.class);
    Root<Orderable> latestOrderablesRoot = latestOrderablesQuery.from(Orderable.class);
    latestOrderablesRoot.alias("latest");

    latestOrderablesQuery.select(
        builder.concat(
            latestOrderablesRoot.get(IDENTITY).get(ID).as(String.class),
            builder.max(latestOrderablesRoot.get(IDENTITY).get(VERSION_NUMBER)).as(String.class)));
    latestOrderablesQuery.groupBy(latestOrderablesRoot.get(IDENTITY).get(ID));

    return latestOrderablesQuery;
  }

  private Query getLastUpdatedQuery(SearchParams searchParams, boolean count) {
    String startNativeQuery = count ? NATIVE_COUNT_LAST_UPDATED : NATIVE_SELECT_LAST_UPDATED;
    StringBuilder builder = new StringBuilder(startNativeQuery);
    List<String> wheres = Lists.newArrayList();
    String queryCondition;

    if (null != searchParams) {
      if (null != searchParams.getProgramCode()) {
        builder.append(NATIVE_PROGRAM_ORDERABLE_INNER_JOIN + NATIVE_PROGRAM_INNER_JOIN);
        queryCondition = "LOWER (p.code) LIKE '%"
            + searchParams.getProgramCode().toLowerCase() + "%'";
        wheres.add(queryCondition);
      }

      if (null != searchParams.getCode()) {
        queryCondition = "LOWER (o.code) LIKE '%"
            + searchParams.getCode().toLowerCase() + "%'";
        wheres.add(queryCondition);
      }

      if (null != searchParams.getName()) {
        queryCondition = "LOWER (o.fullproductname) LIKE '%"
            + searchParams.getName().toLowerCase() + "%'";
        wheres.add(queryCondition);
      }

      if (!wheres.isEmpty()) {
        builder.append(WHERE).append(String.join(AND, wheres));
      }
    }

    if (!count) {
      builder.append(ORDER_BY_LAST_UPDATED_DESC_LIMIT_1);
    }
    XLOGGER.info("QueryParamString: " + builder.toString());
    return entityManager.createNativeQuery(builder.toString());
  }

  private List<VersionIdentity> convertPairToVersionIdentity(Set<Pair<UUID, Long>> identities) {

    if (identities.isEmpty()) {
      return Collections.emptyList();
    }

    return identities
        .stream()
        .map(identity -> new VersionIdentity(identity.getLeft(), identity.getRight()))
        .collect(Collectors.toList());
  }

  // appropriate class has been passed in the EntityManager.createQuery method
  @SuppressWarnings("unchecked")
  private List<Orderable> retrieveOrderables(Collection<VersionIdentity> identities,
      Boolean date) {
    CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Orderable> criteriaQuery =
        criteriaBuilder.createQuery(Orderable.class);
    Root<Orderable> root = criteriaQuery.from(Orderable.class);
    criteriaQuery.select(root).where(root.get(IDENTITY).in(identities));

    if (date) {
      criteriaQuery.orderBy(criteriaBuilder.desc(root.get("lastUpdated")));
    }

    return entityManager
        .createQuery(criteriaQuery)
        .unwrap(org.hibernate.Query.class)
        .setResultTransformer(DistinctRootEntityResultTransformer.INSTANCE)
        .list();
  }

}
