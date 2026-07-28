<template>
  <div class="community-page">
    <header class="page-heading"><div><div class="page-eyebrow">CONTENT POLICY</div><h1>内容规则</h1><p>维护关键词、内容范围、风险等级与自动处理动作。</p></div><n-button type="primary" @click="openCreate">新增规则</n-button></header>
    <n-card><n-space style="margin-bottom:16px"><n-select v-model:value="status" :options="statusOptions" clearable placeholder="全部状态" style="width:150px" @update:value="load" /><n-button @click="load">刷新</n-button></n-space><n-data-table :data="rows" :columns="columns" :loading="loading" :row-key="(row: ContentRule) => row.id" /></n-card>
    <n-modal v-model:show="visible" preset="card" :title="editing ? '编辑规则' : '新增规则'" style="width:640px"><n-form label-placement="top"><n-form-item label="关键词" required><n-input v-model:value="form.keyword" :disabled="!!editing" /></n-form-item><n-form-item label="内容范围"><n-checkbox-group v-model:value="form.scopes"><n-space><n-checkbox value="ARTICLE" label="文章"/><n-checkbox value="COMMENT" label="评论"/><n-checkbox value="MOMENT" label="动态"/></n-space></n-checkbox-group></n-form-item><n-grid :cols="2" :x-gap="16"><n-form-item-gi label="风险等级"><n-select v-model:value="form.riskLevel" :options="riskOptions"/></n-form-item-gi><n-form-item-gi label="命中动作"><n-select v-model:value="form.hitAction" :options="actionOptions"/></n-form-item-gi></n-grid><n-form-item label="说明"><n-input v-model:value="form.description" type="textarea" :maxlength="500"/></n-form-item><n-space justify="end"><n-button @click="visible=false">取消</n-button><n-button type="primary" @click="save">保存</n-button></n-space></n-form></n-modal>
  </div>
</template>
<script setup lang="ts">
import { h, onMounted, ref } from 'vue'
import { NButton, NTag, type DataTableColumns, useDialog, useMessage } from 'naive-ui'
import { communityApi, type ContentRule } from '@/api/community'
const rows=ref<ContentRule[]>([]), loading=ref(false), visible=ref(false), editing=ref<ContentRule|null>(null), status=ref<string|null>(null), message=useMessage(), dialog=useDialog()
const statusOptions=[{label:'启用',value:'ACTIVE'},{label:'停用',value:'DISABLED'}],riskOptions=['LOW','MEDIUM','HIGH','CRITICAL'].map(value=>({label:value,value})),actionOptions=['WARN','MANUAL_REVIEW','BLOCK'].map(value=>({label:value,value}))
const form=ref({keyword:'',scopes:['ARTICLE','COMMENT','MOMENT'],riskLevel:'MEDIUM',hitAction:'WARN',description:''})
async function load(){loading.value=true;try{rows.value=await communityApi.contentRules(status.value||undefined)}finally{loading.value=false}}
function openCreate(){editing.value=null;form.value={keyword:'',scopes:['ARTICLE','COMMENT','MOMENT'],riskLevel:'MEDIUM',hitAction:'WARN',description:''};visible.value=true}
function edit(row:ContentRule){editing.value=row;form.value={keyword:row.keyword,scopes:row.contentScopes.split(','),riskLevel:row.riskLevel,hitAction:row.hitAction,description:row.description||''};visible.value=true}
async function save(){if(!form.value.keyword.trim()||!form.value.scopes.length){message.error('请填写关键词并选择范围');return}const payload={keyword:form.value.keyword,contentScopes:form.value.scopes.join(','),riskLevel:form.value.riskLevel,hitAction:form.value.hitAction,description:form.value.description||null};if(editing.value)await communityApi.updateContentRule(editing.value.id,payload);else await communityApi.createContentRule({...payload,status:'ACTIVE',sortOrder:0});visible.value=false;await load();message.success('已保存')}
function remove(row:ContentRule){dialog.warning({title:'删除规则',content:`确认删除“${row.keyword}”吗？`,positiveText:'删除',negativeText:'取消',async onPositiveClick(){await communityApi.deleteContentRule(row.id);await load()}})}
const columns:DataTableColumns<ContentRule>=[{title:'关键词',key:'keyword'},{title:'范围',key:'contentScopes'},{title:'风险',key:'riskLevel'},{title:'动作',key:'hitAction'},{title:'状态',key:'status',render:row=>h(NTag,{type:row.status==='ACTIVE'?'success':'default'},{default:()=>row.status})},{title:'操作',key:'actions',render:row=>h('div',{class:'actions'},[h(NButton,{size:'small',onClick:()=>edit(row)},{default:()=> '编辑'}),h(NButton,{size:'small',type:'error',onClick:()=>remove(row)},{default:()=> '删除'})])}]
onMounted(load)
</script>
<style scoped>.community-page{padding:24px}.page-heading{display:flex;justify-content:space-between;align-items:end;margin-bottom:24px}.page-eyebrow{font-size:12px;color:var(--n-text-color-3)}.page-heading h1{margin:8px 0}.page-heading p{margin:0;color:var(--n-text-color-3)}.actions{display:flex;gap:8px}</style>
