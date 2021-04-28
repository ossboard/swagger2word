package org.word.controller;

import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.word.service.WordService;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.Map;

@Controller
@Api(tags = "the toWord API")
@Slf4j
public class WordController {

    @Value("${swagger.url}")
    private String swaggerUrl;

    @Autowired
    private WordService tableService;
    @Autowired
    private SpringTemplateEngine springTemplateEngine;

    private String fileName = "toWord";

    @Deprecated
    @ApiOperation(value = "swagger 문서를 html 문서로 변환합니다. 웹 페이지를 마우스 오른쪽 버튼으로 클릭하고 xxx.doc로 저장하여 Word 문서로 변환 할 수 있습니다.", response = String.class, tags = {"Word"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "요청 성공", response = String.class)})
    @RequestMapping(value = "/toWord", method = {RequestMethod.GET})
    public String getWord(Model model,
                          @ApiParam(value = "주소", required = false) @RequestParam(value = "url", required = false) String url,
                          @ApiParam(value = "다운로드", required = false) @RequestParam(value = "download", required = false, defaultValue = "1") Integer download) {
        generateModelData(model, url, download);

        // Is there a localized template available ?
        Locale currentLocale = Locale.getDefault();
        String localizedTemplate = "word-" + currentLocale.getLanguage() + "_" + currentLocale.getCountry();
        String fileName = "/templates/" + localizedTemplate + ".html";

        if (getClass().getResourceAsStream(fileName) != null) {
            log.info(fileName + " resource found");
            return localizedTemplate;
        } else {
            log.info(fileName + " resource not found, using default");
        }
        return "word";
    }

    private void generateModelData(Model model, String url, Integer download) {
        url = StringUtils.defaultIfBlank(url, swaggerUrl);
        Map<String, Object> result = tableService.tableList(url);
        model.addAttribute("url", url);
        model.addAttribute("download", download);
        model.addAllAttributes(result);
    }

    @ApiOperation(value = "swagger 문서를 문서 문서로 원 클릭 다운로드", notes = "", tags = {"Word"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "요청 성공")})
    @RequestMapping(value = "/downloadWord", method = {RequestMethod.GET})
    public void word(Model model, @ApiParam(value = "주소", required = false) @RequestParam(required = false) String url, HttpServletResponse response) {
        generateModelData(model, url, 0);
        writeContentToResponse(model, response);
    }

    private void writeContentToResponse(Model model, HttpServletResponse response) {
        Context context = new Context();
        context.setVariables(model.asMap());
        String content = springTemplateEngine.process("word", context);
        response.setContentType("application/octet-stream;charset=utf-8");
        response.setCharacterEncoding("utf-8");
        try (BufferedOutputStream bos = new BufferedOutputStream(response.getOutputStream())) {
            response.setHeader("Content-disposition", "attachment;filename=" + URLEncoder.encode(fileName + ".doc", "utf-8"));
            byte[] bytes = content.getBytes();
            bos.write(bytes, 0, bytes.length);
            bos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @ApiOperation(value = "swagger json 파일을 워드 문서로 변환하고 다운로드", notes = "", tags = {"Word"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "요청 성공")})
    @RequestMapping(value = "/fileToWord", method = {RequestMethod.POST})
    public void getWord(Model model, @ApiParam("swagger json file") @Valid @RequestPart("jsonFile") MultipartFile jsonFile, HttpServletResponse response) {
        generateModelData(model, jsonFile);
        writeContentToResponse(model, response);
    }

    @ApiOperation(value = "swagger json 문자열을 워드 문서로 변환하고 다운로드", notes = "", tags = {"Word"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "요청 성공")})
    @RequestMapping(value = "/strToWord", method = {RequestMethod.POST})
    public void getWord(Model model, @ApiParam("swagger json string") @Valid @RequestParam("jsonStr") String jsonStr, HttpServletResponse response) {
        generateModelData(model, jsonStr);
        writeContentToResponse(model, response);
    }

    private void generateModelData(Model model, String jsonStr) {
        Map<String, Object> result = tableService.tableListFromString(jsonStr);
        model.addAttribute("url", "http://");
        model.addAttribute("download", 0);
        model.addAllAttributes(result);
    }

    private void generateModelData(Model model, MultipartFile jsonFile) {
        Map<String, Object> result = tableService.tableList(jsonFile);
        fileName = jsonFile.getOriginalFilename();

        if (fileName != null) {
            fileName = fileName.replaceAll(".json", "");
        } else {
            fileName = "toWord";
        }

        model.addAttribute("url", "http://");
        model.addAttribute("download", 0);
        model.addAllAttributes(result);
    }
}
