package com.aiexam.system.controller;

import com.aiexam.common.AjaxResult;
import com.aiexam.common.annotation.OperationLog;
import com.aiexam.common.constant.RoleConstants;
import com.aiexam.common.vo.PageVO;
import com.aiexam.system.dto.DictDataAddDTO;
import com.aiexam.system.dto.DictDataQueryDTO;
import com.aiexam.system.dto.DictDataUpdateDTO;
import com.aiexam.system.service.SysDictDataService;
import com.aiexam.system.vo.DictDataVO;
import jakarta.validation.Valid;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数据字典数据控制器（管理端 CRUD + 登录即可的字典项查询）
 */
@RestController
@RequestMapping("/dict/data")
public class DictDataController {

    @Autowired
    private SysDictDataService sysDictDataService;

    /**
     * 字典数据分页查询（按字典编码必填）
     */
    @GetMapping("/page")
    @RequiresRoles(RoleConstants.ADMIN)
    public AjaxResult<PageVO<DictDataVO>> page(@Valid DictDataQueryDTO dto) {
        return AjaxResult.success(sysDictDataService.pageDatas(dto));
    }

    /**
     * 按字典编码取启用字典项（登录即可，供前端下拉，走缓存）
     */
    @GetMapping("/list/{dictTypeCode}")
    public AjaxResult<List<DictDataVO>> listByType(@PathVariable String dictTypeCode) {
        return AjaxResult.success(sysDictDataService.listByTypeCode(dictTypeCode));
    }

    /**
     * 字典数据详情
     */
    @GetMapping("/{id}")
    @RequiresRoles(RoleConstants.ADMIN)
    public AjaxResult<DictDataVO> detail(@PathVariable Long id) {
        return AjaxResult.success(sysDictDataService.getDetail(id));
    }

    /**
     * 新增字典项（类型须存在且启用，同类型下值唯一）
     */
    @PostMapping
    @RequiresRoles(RoleConstants.ADMIN)
    @OperationLog(module = "字典管理", action = "新增字典项")
    public AjaxResult<Long> add(@Valid @RequestBody DictDataAddDTO dto) {
        return AjaxResult.success(sysDictDataService.addData(dto));
    }

    /**
     * 修改字典项（不允许更换属主类型）
     */
    @PutMapping
    @RequiresRoles(RoleConstants.ADMIN)
    @OperationLog(module = "字典管理", action = "修改字典项")
    public AjaxResult<Void> update(@Valid @RequestBody DictDataUpdateDTO dto) {
        sysDictDataService.updateData(dto);
        return AjaxResult.success();
    }

    /**
     * 删除字典项
     */
    @DeleteMapping("/{id}")
    @RequiresRoles(RoleConstants.ADMIN)
    @OperationLog(module = "字典管理", action = "删除字典项")
    public AjaxResult<Void> delete(@PathVariable Long id) {
        sysDictDataService.deleteData(id);
        return AjaxResult.success();
    }
}
