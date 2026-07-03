package com.example.moneyquest.app.domain.model;

import lombok.Data;

@Data
public class UserDto {
	private Integer userId;
	private String userName;
	private Integer authority;
	private String mailAddress;

}
