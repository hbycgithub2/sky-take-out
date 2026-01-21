package com.sky.utils;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Base64;

/**
 * 模拟微信支付工具类
 * 用于开发测试环境，模拟微信支付API的行为
 */
@Component
@Slf4j
public class MockWeChatPayUtil {
    
    /**
     * 模拟JSAPI下单
     * 模拟微信支付统一下单接口，生成预支付交易单
     * 
     * @param orderNum 商户订单号
     * @param total 支付金额（元）
     * @param description 商品描述
     * @param openid 用户openid
     * @return 支付参数
     */
    public JSONObject pay(String orderNum, BigDecimal total, String description, String openid) {
        log.info("========== 【模拟支付】开始 ==========");
        log.info("【模拟支付】订单号：{}", orderNum);
        log.info("【模拟支付】金额：{}元", total);
        log.info("【模拟支付】描述：{}", description);
        log.info("【模拟支付】用户openid：{}", openid);
        
        // 模拟生成prepay_id（预支付交易会话标识）
        // 格式：wx + 时间戳 + 随机数
        String prepayId = "wx" + System.currentTimeMillis() + RandomStringUtils.randomNumeric(10);
        log.info("【模拟支付】生成prepay_id：{}", prepayId);
        
        // 模拟生成支付参数
        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonceStr = RandomStringUtils.randomAlphanumeric(32);
        String packageStr = "prepay_id=" + prepayId;
        
        // 模拟签名（真实环境需要使用商户私钥签名）
        String signData = timeStamp + nonceStr + packageStr;
        String paySign = Base64.getEncoder().encodeToString(signData.getBytes());
        
        // 构造返回结果
        JSONObject result = new JSONObject();
        result.put("timeStamp", timeStamp);
        result.put("nonceStr", nonceStr);
        result.put("package", packageStr);
        result.put("signType", "RSA");
        result.put("paySign", paySign);
        result.put("prepay_id", prepayId);
        
        log.info("【模拟支付】生成支付参数成功");
        log.info("========== 【模拟支付】结束 ==========");
        
        return result;
    }
    
    /**
     * 模拟生成支付回调数据
     * 
     * @param orderNumber 订单号
     * @return 回调数据
     */
    public JSONObject mockPayCallback(String orderNumber) {
        log.info("【模拟支付】生成支付回调数据，订单号：{}", orderNumber);
        
        JSONObject callback = new JSONObject();
        callback.put("out_trade_no", orderNumber);
        callback.put("transaction_id", "4200" + System.currentTimeMillis());
        callback.put("trade_state", "SUCCESS");
        callback.put("success_time", System.currentTimeMillis());
        
        return callback;
    }
}
