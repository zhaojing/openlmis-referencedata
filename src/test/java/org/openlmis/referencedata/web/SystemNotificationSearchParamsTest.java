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

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;
import org.openlmis.referencedata.ToStringTestUtils;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.springframework.util.LinkedMultiValueMap;

public class SystemNotificationSearchParamsTest {

  private static final String ACTIVE = "active";
  private static final String AUTHOR_ID = "authorId";

  private LinkedMultiValueMap<String, Object> queryMap = new LinkedMultiValueMap<>();

  @Test
  public void equalsContract() {
    EqualsVerifier
        .forClass(SystemNotificationSearchParams.class)
        .suppress(Warning.NONFINAL_FIELDS)
        .verify();
  }

  @Test
  public void shouldImplementToString() {
    queryMap.add(AUTHOR_ID, "some-id");
    SystemNotificationSearchParams params = new SystemNotificationSearchParams(queryMap);

    ToStringTestUtils
        .verify(SystemNotificationSearchParams.class, params,
            "ACTIVE", "AUTHOR_ID", "ALL_PARAMETERS");
  }

  @Test
  public void shouldReturnTrueIfActiveFlagValueIsTrue() {
    queryMap.add(ACTIVE, true);
    SystemNotificationSearchParams params = new SystemNotificationSearchParams(queryMap);

    assertThat(params.getActive()).isTrue();
  }

  @Test
  public void shouldReturnFalseIfActiveFlagValueIsFalse() {
    queryMap.add(ACTIVE, false);
    SystemNotificationSearchParams params = new SystemNotificationSearchParams(queryMap);

    assertThat(params.getActive()).isFalse();
  }

  @Test
  public void shouldReturnNullIfParamsDoesNotContainActiveFlag() {
    queryMap.clear();
    SystemNotificationSearchParams params = new SystemNotificationSearchParams(queryMap);

    assertThat(params.getActive()).isNull();
  }

  @Test
  public void shouldReturnAuthorId() {
    queryMap.add(AUTHOR_ID, "80236cdb-6958-4fd3-bee6-4713af4370b1");
    SystemNotificationSearchParams params = new SystemNotificationSearchParams(queryMap);

    assertThat(params.getAuthorId().toString()).isEqualTo("80236cdb-6958-4fd3-bee6-4713af4370b1");
  }

  @Test
  public void shouldReturnNullIfParamsDoesNotContainAuthorId() {
    queryMap.clear();
    SystemNotificationSearchParams params = new SystemNotificationSearchParams(queryMap);

    assertThat(params.getAuthorId()).isNull();
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionIfParamsAreInvalid() {
    queryMap.add("invalid-param", "some-value");
    new SystemNotificationSearchParams(queryMap);
  }

}
