package com.tps.service;

import com.tps.exception.SensitiveContentException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 文件说明：集中维护用户自定义内容的敏感词检测规则。
 */
@Service
public class SensitiveWordService {

    public static final String CONTENT_MESSAGE = "内容包含敏感词，请修改后再提交";
    public static final String SEARCH_MESSAGE = "无法搜索请重试";

    private static final List<Set<String>> SENSITIVE_KEYWORD_GROUPS = List.of(
            Set.of("烟", "香烟", "电子烟", "烟草", "vape", "烟弹", "烟油"),
            Set.of("酒", "白酒", "啤酒", "洋酒", "酒精", "威士忌", "伏特加"),
            Set.of("代考", "替考", "考试答案", "答案", "枪手", "代写", "论文代写"),
            Set.of("代课", "替课", "签到", "代签", "刷课", "网课代刷"),
            Set.of("代跑", "跑腿", "代取", "代拿"),
            Set.of("管制刀具", "刀具", "匕首", "甩棍", "弹弓", "电击器", "防狼喷雾"),
            Set.of("校园贷", "贷款", "借贷", "套现", "分期套现", "花呗套现", "白条套现"),
            Set.of("药", "处方药", "违禁药", "迷药", "安眠药", "止痛药", "兴奋剂"),
            Set.of("博彩", "赌博", "下注", "彩票", "赌球", "赌钱"),
            Set.of("色情", "成人视频", "裸聊", "约炮", "陪聊", "黄色资源"),
            Set.of("账号买卖", "游戏账号", "校园卡买卖", "银行卡", "电话卡", "实名账号"),
            Set.of("外挂", "脚本", "破解软件", "盗版软件", "翻墙", "vpn"),
            Set.of("假证", "办证", "学生证", "身份证", "驾驶证", "毕业证"),
            Set.of(
                    "傻逼", "傻叉", "煞笔", "沙币", "蠢货", "废物", "垃圾", "去死", "王八蛋",
                    "妈的", "他妈的", "操你", "草你", "滚蛋", "贱人", "脑残", "白痴", "狗东西",
                    "混蛋", "畜生", "sb", "nmsl"
            )
    );

    public void rejectIfSensitive(String... values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            if (containsSensitive(value)) {
                throw new SensitiveContentException(List.of(
                        new SensitiveContentException.FieldViolation("content", "内容", "内容包含敏感词")
                ));
            }
        }
    }

    public void rejectIfSensitiveFields(FieldContent... fields) {
        if (fields == null) {
            return;
        }
        List<SensitiveContentException.FieldViolation> violations = new ArrayList<>();
        for (FieldContent field : fields) {
            if (field != null && containsSensitive(field.value())) {
                violations.add(new SensitiveContentException.FieldViolation(
                        field.field(),
                        field.label(),
                        field.label() + "包含敏感词"
                ));
            }
        }
        if (!violations.isEmpty()) {
            throw new SensitiveContentException(violations);
        }
    }

    public FieldContent field(String field, String label, String value) {
        return new FieldContent(field, label, value);
    }

    public boolean containsSensitive(String value) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            return false;
        }
        return SENSITIVE_KEYWORD_GROUPS.stream()
                .flatMap(Set::stream)
                .map(this::normalize)
                .anyMatch(normalized::contains);
    }

    public List<String> expandSearchTerms(String keyword) {
        String normalized = normalize(keyword);
        if (normalized.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        terms.add(keyword.trim());
        terms.add(normalized);
        Arrays.stream(normalized.split("\\s+"))
                .filter(token -> !token.isBlank())
                .forEach(terms::add);
        for (Set<String> group : SENSITIVE_KEYWORD_GROUPS) {
            boolean hit = group.stream().map(this::normalize).anyMatch(terms::contains);
            if (hit) {
                terms.addAll(group);
            }
        }
        return terms.stream().filter(term -> !term.isBlank()).toList();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{P}\\p{Z}\\s]+", "")
                .trim();
    }

    public record FieldContent(String field, String label, String value) {
    }
}
