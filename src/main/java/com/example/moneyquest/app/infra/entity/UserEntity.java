package com.example.moneyquest.app.infra.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "user_t")
public class UserEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
	private Integer userId;

	@Column(name = "parent_user_id")
	private Integer parentUserId;

	@Column(name = "authority")
	private Integer authority;

	@Column(name = "user_name")
	private String userName;

	@Column(name = "mail_address")
	private String mailAddress;

	@Column(name = "password")
	private String password;

}
