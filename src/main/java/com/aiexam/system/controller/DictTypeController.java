package com.aiexam.system.controller;

import com.aiexam.common.AjaxResult;
import com.aiexam.common.annotation.OperationLog;
import com.aiexam.common.vo.PageVO;
import com.aiexam.system.dto.DictTypeAddDTO;
import com.aiexam.system.dto.DictTypeQueryDTO;
import com.aiexam.system.dto.DictTypeUpdateDTO;
import com.aiexam.system.service.SysDictTypeService;
import com.aiexam.system.vo.DictTypeVO;
import jakarta.validation.Valid;
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
 * 数据字典类型控制器（管理端）
 */
@RestController
@RequestMapping("/dict/type")
public class DictTypeController {

    @Autowired
    private SysDictTypeService sysDictTypeService;

    /**
     * 字典类型分页查询（名称/编码模糊 + 状态）
     */
    @GetMapping("/page")
    public AjaxResult<PageVO<DictTypeVO>> page(DictTypeQueryDTO dto) {
        return AjaxResult.success(sysDictTypeService.pageTypes(dto));
    }

    /**
     * 全量字典类型（类型下拉用）
     */
    @GetMapping("/list")
    public AjaxResult<List<DictTypeVO>> list() {
        return AjaxResult.success(sysDictTypeService.listAll());
    }

    /**
     * 字典类型详情
     */
    @GetMapping("/{id}")
    public AjaxResult<DictTypeVO> detail(@PathVariable Long id) {
        return AjaxResult.success(sysDictTypeService.getDetail(id));
    }

    /**
     * 新增字典类型（编码唯一）
     */
    @PostMapping
    @OperationLog(module = "字典管理", action = "新增字典类型")
    public AjaxResult<Long> add(@Valid @RequestBody DictTypeAddDTO dto) {
        return AjaxResult.success(sysDictTypeService.addType(dto));
    }

    /**
     * 修改字典类型（编码不可修改）
     */
    @PutMapping
    @OperationLog(module = "字典管理", action = "修改字典类型")
    public AjaxResult<Void> update(@Valid @RequestBody DictTypeUpdateDTO dto) {
        sysDictTypeService.updateType(dto);
        return AjaxResult.success();
    }

    /**
     * 删除字典类型（级联删除其下字典数据）
     */
    @DeleteMapping("/{id}")
    @OperationLog(module = "字典管理", action = "删除字典类型")
    public AjaxResult<Void> delete(@PathVariable Long id) {
        sysDictTypeService.deleteType(id);
        return AjaxResult.success();
    }
}
