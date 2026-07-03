package com.example.moneyquest.app.domain.model;

import lombok.Data;

@Data
public class BadgeDto {

	private String code;
	private String name;
	private String description;
	private String icon;
	private Integer requiredCount;
	private Boolean earned;

}
