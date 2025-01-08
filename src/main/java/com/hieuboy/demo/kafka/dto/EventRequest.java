package com.hieuboy.demo.kafka.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.springframework.aot.hint.annotation.Reflective;

import java.io.Serializable;

@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Reflective
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventRequest<ZimJi> implements Serializable {

    String event;

    Long companyId;

    ZimJi payload;

    String responseTopic;

}
