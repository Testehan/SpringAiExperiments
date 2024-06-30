package com.testehan.springai.mongoatlas.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class Tomatoes {
    private Viewer viewer;
    private Date lastUpdated;
}
