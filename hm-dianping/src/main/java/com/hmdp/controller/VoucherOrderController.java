package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.service.IVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/voucher-order")
@Slf4j
public class VoucherOrderController {
    @Resource
    private IVoucherOrderService voucherOrderService;




//    @PostMapping("seckill/{id}")
//    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
//        return voucherOrderService.seckillVoucher(voucherId);
//    }

    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        long startTime = System.currentTimeMillis();
        Result  result = voucherOrderService.seckillVoucher(voucherId);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        log.info("秒杀接口耗时：{}ms",duration);
//        return voucherOrderService.seckillVoucher(voucherId);
        return result;
    }
}
