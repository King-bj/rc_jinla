package com.notifyhub.repository;

import com.notifyhub.entity.NotificationVendorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/** 供应商配置。编码即主键。 */
public interface NotificationVendorRepository extends JpaRepository<NotificationVendorEntity, String> {
}
