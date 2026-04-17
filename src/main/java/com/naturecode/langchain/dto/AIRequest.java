package com.naturecode.langchain.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AIRequest {
  private String userId;
  private String question;
}
