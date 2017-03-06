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

package org.openlmis.referencedata.validate;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.openlmis.referencedata.validate.ProgramValidator.CODE;
import static org.openlmis.referencedata.validate.ValidationTestUtils.assertErrorMessage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.Code;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.dto.ProgramDto;
import org.openlmis.referencedata.repository.ProgramRepository;
import org.openlmis.referencedata.util.messagekeys.ProgramMessageKeys;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class ProgramValidatorTest {

  @Mock
  private ProgramRepository programRepository;

  @InjectMocks
  private Validator validator = new ProgramValidator();

  private ProgramDto programDto;
  private Errors errors;

  @Before
  public void setUp() throws Exception {
    programDto = new ProgramDto();
    programDto.setCode("ProgramCode");
    programDto.setId(UUID.randomUUID());

    errors = new BeanPropertyBindingResult(programDto, "programDto");
  }

  @Test
  public void shouldNotFindErrors() throws Exception {
    validator.validate(programDto, errors);

    assertThat(errors.getErrorCount(), is(equalTo(0)));
  }

  @Test
  public void shouldRejectIfCodeIsDuplicated() {
    doReturn(mock(Program.class))
        .when(programRepository)
        .findByCode(Code.code(programDto.getCode()));

    validator.validate(programDto, errors);

    assertErrorMessage(errors, CODE, ProgramMessageKeys.ERROR_CODE_DUPLICATED);
  }

  @Test
  public void shouldNotRejectIfCodeIsDuplicatedAndIdsAreSame() {
    Program old = new Program(programDto.getCode());
    old.setId(programDto.getId());

    doReturn(old)
        .when(programRepository)
        .findByCode(Code.code(programDto.getCode()));

    validator.validate(programDto, errors);
    assertThat(errors.hasFieldErrors(CODE), is(false));
  }

  @Test
  public void shouldRejectIfCodeIsDuplicatedAndIdsAreDifferent() {
    Program old = new Program(programDto.getCode());
    old.setId(UUID.randomUUID());

    doReturn(old)
        .when(programRepository)
        .findByCode(Code.code(programDto.getCode()));

    validator.validate(programDto, errors);
    assertErrorMessage(errors, CODE, ProgramMessageKeys.ERROR_CODE_DUPLICATED);
  }

  @Test(expected = NullPointerException.class)
  public void shouldThrowNullPointerExceptionWhenTargetIsNull() {
    validator.validate(null, errors);
  }

  @Test(expected = NullPointerException.class)
  public void shouldThrowNullPointerExceptionWhenErrorsIsNull() {
    validator.validate(programDto, null);
  }

  @Test
  public void shouldRejectWhenCodeIsNull() {
    programDto.setCode(null);

    validator.validate(programDto, errors);

    assertErrorMessage(errors, CODE, ProgramMessageKeys.ERROR_CODE_REQUIRED);
  }

  @Test
  public void shouldRejectWhenCodeIsEmpty() {
    programDto.setCode("");

    validator.validate(programDto, errors);

    assertErrorMessage(errors, CODE, ProgramMessageKeys.ERROR_CODE_REQUIRED);
  }

  @Test
  public void shouldRejectWhenCodeIsWhitespace() {
    programDto.setCode(" ");

    validator.validate(programDto, errors);

    assertErrorMessage(errors, CODE, ProgramMessageKeys.ERROR_CODE_REQUIRED);
  }
}
