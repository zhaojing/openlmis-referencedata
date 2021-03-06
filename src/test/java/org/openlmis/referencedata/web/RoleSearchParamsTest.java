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

package org.openlmis.referencedata.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.stream.IntStream;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;
import org.openlmis.referencedata.ToStringTestUtils;
import org.springframework.util.LinkedMultiValueMap;

public class RoleSearchParamsTest {

  private static final String RIGHT_ID = "rightId";

  private LinkedMultiValueMap<String, Object> queryMap = new LinkedMultiValueMap<>();

  @Test
  public void equalsContract() {
    EqualsVerifier
        .forClass(RoleSearchParams.class)
        .suppress(Warning.NONFINAL_FIELDS)
        .verify();
  }

  @Test
  public void shouldImplementToString() {
    queryMap.add(RIGHT_ID, "some-id");
    RoleSearchParams params = new RoleSearchParams(queryMap);

    ToStringTestUtils
        .verify(RoleSearchParams.class, params, "RIGHT_ID", "ALL_PARAMETERS");
  }

  @Test
  public void shouldReturnSetOfRightIds() {
    UUID[] ids = IntStream
        .range(0, 10)
        .mapToObj(idx -> UUID.randomUUID())
        .peek(id -> queryMap.add(RIGHT_ID, id.toString()))
        .toArray(UUID[]::new);

    RoleSearchParams params = new RoleSearchParams(queryMap);

    assertThat(params.getRightIds())
        .hasSize(ids.length)
        .contains(ids);
  }

  @Test
  public void shouldReturnEmptySetOfRightIdsIfNonHaveBeenProvided() {
    queryMap.clear();
    RoleSearchParams params = new RoleSearchParams(queryMap);

    assertThat(params.getRightIds()).isNotNull().isEmpty();
  }

}
