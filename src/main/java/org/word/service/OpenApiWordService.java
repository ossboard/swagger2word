package org.word.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

public interface OpenApiWordService {

    Map<String,Object> tableList(String swaggerUrl);

    Map<String, Object> tableListFromString(String jsonStr) throws IOException;

    Map<String, Object> tableList(MultipartFile jsonFile) throws IOException;
}