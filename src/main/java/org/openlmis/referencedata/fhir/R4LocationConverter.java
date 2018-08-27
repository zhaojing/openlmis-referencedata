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

package org.openlmis.referencedata.fhir;

import java.math.BigDecimal;
import java.util.UUID;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Location.LocationPositionComponent;
import org.hl7.fhir.r4.model.Location.LocationStatus;
import org.hl7.fhir.r4.model.Reference;

class R4LocationConverter extends LocationConverter<org.hl7.fhir.r4.model.Location> {

  @Override
  org.hl7.fhir.r4.model.Location createResource(Location input) {
    return new org.hl7.fhir.r4.model.Location();
  }

  @Override
  void setName(org.hl7.fhir.r4.model.Location resource, Location input) {
    resource.setName(input.getName());
  }

  @Override
  void setPhysicalType(org.hl7.fhir.r4.model.Location resource, Location input) {
    CodeableConcept physicalType = new CodeableConcept();
    input
        .getPhysicalType()
        .getCoding()
        .stream()
        .map(elem -> {
          Coding coding = new Coding();
          coding.setSystem(elem.getSystem());
          coding.setCode(elem.getCode());
          coding.setDisplay(elem.getDisplay());

          return coding;
        })
        .forEach(physicalType::addCoding);

    resource.setPhysicalType(physicalType);
  }

  @Override
  void setPartOf(org.hl7.fhir.r4.model.Location resource, Location input) {
    resource.setPartOf(new Reference(input.getPartOf().getReference()));
  }

  @Override
  void setIdentifier(org.hl7.fhir.r4.model.Location resource, Location input) {
    input
        .getIdentifier()
        .stream()
        .map(elem -> {
          Identifier identifier = new Identifier();
          identifier.setSystem(elem.getSystem());
          identifier.setValue(elem.getValue());

          return identifier;
        })
        .forEach(resource::addIdentifier);
  }

  @Override
  void addSystemIdentifier(org.hl7.fhir.r4.model.Location resource,
      String system, UUID value) {
    Identifier identifier = new Identifier();
    identifier.setSystem(system);
    identifier.setValue(value.toString());

    resource.addIdentifier(identifier);
  }

  @Override
  void setAlias(org.hl7.fhir.r4.model.Location resource, Location input) {
    input
        .getAlias()
        .forEach(resource::addAlias);
  }

  @Override
  void setPosition(org.hl7.fhir.r4.model.Location resource, Location input) {
    if (null != input.getPosition()) {
      LocationPositionComponent position = new LocationPositionComponent();
      position.setLatitude(new BigDecimal(input.getPosition().getLatitude()));
      position.setLongitude(new BigDecimal(input.getPosition().getLongitude()));

      resource.setPosition(position);
    }
  }

  @Override
  void setDescription(org.hl7.fhir.r4.model.Location resource, Location input) {
    resource.setDescription(input.getDescription());
  }

  @Override
  void setStatus(org.hl7.fhir.r4.model.Location resource, Location input) {
    if (null != input.getStatus()) {
      try {
        resource.setStatus(LocationStatus.fromCode(input.getStatus()));
      } catch (FHIRException exp) {
        throw new IllegalStateException(exp);
      }
    }
  }

}