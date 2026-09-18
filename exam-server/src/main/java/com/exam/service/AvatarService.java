package com.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.common.BizException;
import com.exam.entity.SysUserAvatar;
import com.exam.mapper.SysUserAvatarMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
/** 当前登录用户自己的头像：写入 MySQL，不进账号列表查询。 */
public class AvatarService {
    private static final long MAX_BYTES = 2 * 1024 * 1024;

    private final SysUserAvatarMapper avatarMapper;

    public boolean hasAvatar(Long userId) {
        return userId != null && avatarMapper.selectCount(
                new LambdaQueryWrapper<SysUserAvatar>().eq(SysUserAvatar::getUserId, userId)) > 0;
    }

    public ResponseEntity<byte[]> image(Long userId) {
        SysUserAvatar row = avatarMapper.selectById(userId);
        if (row == null || row.getPhoto() == null || row.getPhoto().length == 0) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(row.getContentType()));
        headers.setCacheControl(CacheControl.noStore());
        headers.setContentLength(row.getPhoto().length);
        return new ResponseEntity<>(row.getPhoto(), headers, HttpStatus.OK);
    }

    @Transactional
    public void save(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择图片");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BizException("图片不能超过 2MB");
        }
        String type = contentType(file);
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BizException("读取图片失败");
        }
        LocalDateTime now = LocalDateTime.now();
        SysUserAvatar row = avatarMapper.selectById(userId);
        if (row == null) {
            row = new SysUserAvatar();
            row.setUserId(userId);
            row.setContentType(type);
            row.setPhoto(bytes);
            row.setUpdatedAt(now);
            avatarMapper.insert(row);
        } else {
            row.setContentType(type);
            row.setPhoto(bytes);
            row.setUpdatedAt(now);
            avatarMapper.updateById(row);
        }
    }

    @Transactional
    public void delete(Long userId) {
        avatarMapper.deleteById(userId);
    }

    private String contentType(MultipartFile file) {
        String raw = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if ("image/jpeg".equals(raw) || "image/jpg".equals(raw) || name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if ("image/png".equals(raw) || name.endsWith(".png")) {
            return "image/png";
        }
        if ("image/webp".equals(raw) || name.endsWith(".webp")) {
            return "image/webp";
        }
        if ("image/gif".equals(raw) || name.endsWith(".gif")) {
            return "image/gif";
        }
        throw new BizException("只支持 jpg / png / gif / webp");
    }
}
