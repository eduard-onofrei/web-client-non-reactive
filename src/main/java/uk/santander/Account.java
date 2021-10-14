package uk.santander;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder(toBuilder = true)
public class Account {

    private String id;
    private final String owner;
    private final Double value;
    private final double result;
}