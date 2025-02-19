package com.hmdp.utils;


import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class SentinelTest {

    public static void main(String[] args) {
        initFlowRules();
        while(true){
            try(Entry entry = SphU.entry("hello")){
                System.out.println("hello sentinel");
            }catch (Exception e){
                System.out.println("blocked");
//                e.printStackTrace();
            }
        }
    }

    private static void initFlowRules(){
        // 1. 定义规则
        List<FlowRule> rules = new ArrayList<>();
        FlowRule rule = new FlowRule();
        rule.setResource("hello");
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(20);
        rules.add(rule);
        // 2. 加载规则
        FlowRuleManager.loadRules(rules);
    }
}
