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

package org.openlmis.referencedata.domain;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "timeZoneId")
@Table(name = "system_notifications")
public class SystemNotification extends BaseEntity {

  @Transient
  private String timeZoneId;

  private String title;

  @Column(nullable = false)
  private String message;

  private ZonedDateTime startDate;

  private ZonedDateTime expiryDate;

  @Column(nullable = false)
  private ZonedDateTime createdDate;

  @Column(nullable = false, columnDefinition = "boolean DEFAULT true")
  private boolean active;

  @ManyToOne
  @JoinColumn(name = "authorid", nullable = false)
  private User author;

  private boolean displayed;

  /**
   * Default constructor.
   *
   * @param title system notification title
   * @param message system notification message
   * @param startDate system notification start date
   * @param expiryDate system notification expiry date
   * @param createdDate system notification create date
   * @param active true if system notification is active
   * @param author system notification author
   * @param displayed true if system notification is displayed
   */
  public SystemNotification(String title, String message, ZonedDateTime startDate,
      ZonedDateTime expiryDate, ZonedDateTime createdDate, boolean active, User author,
      boolean displayed) {
    this.setTitle(title);
    this.setMessage(message);
    this.setStartDate(startDate);
    this.setExpiryDate(expiryDate);
    this.setCreatedDate(createdDate);
    this.setActive(active);
    this.setAuthor(author);
    this.setDisplayed(displayed);
  }

  /**
   * Creates new system notification object based on data from {@link Importer}
   * and author argument.
   *
   * @param importer instance of {@link Importer}
   * @param author notification author to set.
   * @return new instance of facility.
   */
  public static SystemNotification newInstance(Importer importer, User author) {
    SystemNotification systemNotification = new SystemNotification();
    systemNotification.setId(importer.getId());
    systemNotification.setTitle(importer.getTitle());
    systemNotification.setMessage(importer.getMessage());
    systemNotification.setStartDate(importer.getStartDate());
    systemNotification.setExpiryDate(importer.getExpiryDate());
    systemNotification.setCreatedDate(importer.getCreatedDate());
    systemNotification.setActive(importer.isActive());
    systemNotification.setAuthor(author);
    systemNotification.setDisplayed(importer.isDisplayed());
    return systemNotification;
  }

  /**
   * Exports current state of system notification object.
   *
   * @param exporter instance of {@link Exporter}
   */
  public void export(Exporter exporter) {
    exporter.setId(id);
    exporter.setTitle(title);
    exporter.setMessage(message);
    exporter.setStartDate(startDate);
    exporter.setExpiryDate(expiryDate);
    exporter.setCreatedDate(createdDate);
    exporter.setActive(active);
    exporter.setDisplayed(displayed);
    if (author != null) {
      exporter.setAuthor(author);
    }
  }

  public interface Exporter extends BaseExporter {

    void setTitle(String title);

    void setMessage(String message);

    void setStartDate(ZonedDateTime startDate);

    void setExpiryDate(ZonedDateTime expiryDate);

    void setCreatedDate(ZonedDateTime createdDate);

    void setAuthor(User author);

    void setActive(boolean active);

    void setDisplayed(boolean displayed);
  }

  public interface Importer extends BaseImporter {

    String getTitle();

    String getMessage();

    ZonedDateTime getStartDate();

    ZonedDateTime getExpiryDate();

    ZonedDateTime getCreatedDate();

    UUID getAuthorId();

    boolean isActive();

    boolean isDisplayed();
  }

  @PrePersist
  private void prePersist() {
    this.createdDate = ZonedDateTime.now();
    setDisplayed();
  }

  @PreUpdate
  private void preUpdate() {
    setDisplayed();
  }

  private void setDisplayed() {
    if (expiryDate != null && startDate != null) {
      this.displayed = active && expiryDate.isAfter(ZonedDateTime.now(ZoneId.of(timeZoneId)))
          && startDate.isBefore(ZonedDateTime.now(ZoneId.of(timeZoneId)));
    } else if (startDate != null) {
      this.displayed = active && startDate.isBefore(ZonedDateTime.now(ZoneId.of(timeZoneId)));
    } else if (expiryDate != null) {
      this.displayed = active && expiryDate.isAfter(ZonedDateTime.now(ZoneId.of(timeZoneId)));
    } else {
      this.displayed = active;
    }
  }

}
