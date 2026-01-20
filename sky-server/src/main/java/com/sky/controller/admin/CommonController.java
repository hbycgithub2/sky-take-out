package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/admin/common")
@Slf4j
@Api(tags = "通用接口")
public class CommonController {
    @Autowired
    private AliOssUtil aliOssUtil;

    /**
     * 文件上传
     *
     * @param file
     * @return
     */
    @PostMapping("/upload")
    @ApiOperation("文件上传")
    public Result<String> upload(MultipartFile file) {
        log.info(file.getName());

/*        //原始文件名
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));

        //将文件上传的阿里云
        String fileName = UUID.randomUUID().toString() + extension;
        try {
            String filePath = aliOssUtil.upload(file.getBytes(), fileName);
            return Result.success(filePath);
        } catch (IOException e) {
            log.error("文件上传失败:{}", e.getMessage());
        }

        return Result.error(MessageConstant.UPLOAD_FAILED);
    }*/


//file校验
        if (file.isEmpty()) {
            return Result.error(MessageConstant.UPLOAD_FAILED);
        }
//file重命名(a:1.png b:1.png)
        String originalFilename = file.getOriginalFilename();//原始的图片名
        String ext = "." + originalFilename.split("\\.")[1];// 1.png
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String fileName = uuid + ext;
//上传图片
        ApplicationHome applicationHome = new ApplicationHome(this.getClass());
        String pre = applicationHome.getDir().getParentFile().getParentFile().getAbsolutePath() +
                "\\src\\main\\images\\";
        String path = pre + fileName;
        try {
            file.transferTo(new File(path));
            // 返回可访问的URL路径，而不是本地文件路径
            String imageUrl = "/images/" + fileName;
            log.info("图片上传成功，访问路径：{}", imageUrl);
            return Result.success(imageUrl);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("图片上传失败：{}", e.getMessage());
            return Result.error(MessageConstant.UPLOAD_FAILED);
        }
    }
}
