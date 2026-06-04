<template>
  <main class="app-shell">
    <header class="app-header">
      <div class="brand">
        <div class="brand-mark">CP</div>
        <div>
          <h1>CareerPilot</h1>
          <p>AI 简历分析与模拟面试平台</p>
        </div>
      </div>

      <div class="header-actions">
        <el-tag type="success" effect="light">Spring Boot + Vue3</el-tag>
        <el-tag v-if="currentUser" type="info" effect="light">已登录：{{ currentUser.username }}</el-tag>
        <el-button v-if="currentUser" plain @click="logout">退出</el-button>
        <el-button v-else type="primary" plain @click="openAuth('login')">登录 / 注册</el-button>
        <el-button :type="aiStatusButtonType" plain @click="settingsVisible = true">
          {{ aiStatusLabel }}
        </el-button>
        <el-button plain @click="recordsVisible = true">最近记录</el-button>
      </div>
    </header>

    <section class="studio-grid">
      <aside class="studio-panel source-panel">
        <PanelHeader title="简历与岗位" subtitle="上传简历，粘贴目标 JD" />
        <div class="panel-scroll">
          <el-upload
            class="upload-box"
            drag
            :auto-upload="false"
            :show-file-list="false"
            accept=".pdf,.docx,.txt"
            :on-change="handleFileChange"
          >
            <div class="upload-mark">PDF</div>
            <div class="upload-title">拖拽或点击上传简历</div>
            <div class="upload-subtitle">{{ resumeName || '支持 PDF / DOCX / TXT，也可以直接粘贴文本' }}</div>
          </el-upload>

          <el-button class="resume-builder-button" type="primary" plain @click="resumeBuilderVisible = true">
            打开简历编辑器
          </el-button>

          <label class="field-label">简历文本</label>
          <el-input
            v-model="resumeText"
            type="textarea"
            :rows="12"
            resize="none"
            placeholder="粘贴你的简历内容，或上传文件后自动解析"
          />

          <label class="field-label">目标岗位 JD</label>
          <el-input
            v-model="jobDescription"
            type="textarea"
            :rows="8"
            resize="none"
            placeholder="例如：Java 后端开发实习生，要求熟悉 Java、Spring Boot、MySQL、RESTful API..."
          />
        </div>

        <div class="panel-footer">
          <el-button type="primary" :loading="analyzing" @click="handleAnalyze">开始分析</el-button>
          <el-button :loading="interviewing" @click="handleInterview">生成面试题</el-button>
        </div>
      </aside>

      <section class="studio-panel analysis-panel">
        <PanelHeader title="分析结果" subtitle="匹配度、短板和简历优化建议" />
        <div class="panel-scroll">
          <div v-if="!analysis" class="empty-card">
            <strong>等待分析</strong>
            <span>填写简历和 JD 后，点击左侧“开始分析”。</span>
          </div>

          <template v-else>
            <section class="score-card">
              <div class="score-number">
                <strong>{{ analysis.matchScore }}</strong>
                <span>分</span>
              </div>
              <div class="score-copy">
                <div class="score-line">
                  <el-progress :percentage="analysis.matchScore" :show-text="false" :stroke-width="8" />
                  <el-tag :type="scoreTagType" effect="light">{{ scoreLabel }}</el-tag>
                </div>
                <p>{{ analysis.summary }}</p>
              </div>
            </section>

            <section class="stat-grid">
              <div class="stat-card">
                <strong>{{ analysis.strengths?.length || 0 }}</strong>
                <span>匹配优势</span>
              </div>
              <div class="stat-card">
                <strong>{{ analysis.gaps?.length || 0 }}</strong>
                <span>能力短板</span>
              </div>
              <div class="stat-card">
                <strong>{{ analysis.suggestions?.length || 0 }}</strong>
                <span>优化建议</span>
              </div>
            </section>

            <section v-if="prioritySuggestions.length" class="priority-card">
              <div class="block-title">
                <strong>优先处理</strong>
                <el-tag type="danger" effect="plain">招聘会前</el-tag>
              </div>
              <ol>
                <li v-for="item in prioritySuggestions" :key="item">{{ item }}</li>
              </ol>
            </section>

            <el-collapse v-model="openSections" class="clean-collapse">
              <el-collapse-item title="匹配优势" name="strengths">
                <ResultList :items="analysis.strengths" type="success" />
              </el-collapse-item>
              <el-collapse-item title="能力短板" name="gaps">
                <ResultList :items="analysis.gaps" type="warning" />
              </el-collapse-item>
              <el-collapse-item title="优化建议" name="suggestions">
                <ResultList :items="analysis.suggestions" type="primary" />
              </el-collapse-item>
              <el-collapse-item title="项目写法" name="projectRewrite">
                <ResultList :items="analysis.projectRewrite" type="info" />
              </el-collapse-item>
            </el-collapse>
          </template>
        </div>
      </section>

      <aside class="studio-panel interview-panel">
        <PanelHeader title="AI 助手" subtitle="模拟面试与上下文对话" />
        <el-tabs v-model="rightTab" class="right-tabs">
          <el-tab-pane label="模拟面试" name="interview">
            <div class="assistant-pane">
              <div class="tab-scroll">
                <div v-if="questions.length === 0" class="empty-card">
                  <strong>暂无面试题</strong>
                  <span>点击“生成面试题”，这里会显示可练习的问题。</span>
                </div>

                <ol v-else class="interview-list">
                  <li v-for="(question, index) in questions" :key="question">
                    <span>{{ index + 1 }}</span>
                    <p>{{ question }}</p>
                  </li>
                </ol>

                <el-alert v-if="interviewFeedback" :title="interviewFeedback" type="info" :closable="false" />
              </div>
              <div class="panel-footer">
                <el-button type="primary" plain :loading="interviewing" @click="handleInterview">重新生成</el-button>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="AI 对话" name="chat">
            <div class="chat-layout">
              <div class="chat-history-panel">
                <div class="chat-history-actions">
                  <el-button size="small" type="primary" plain @click="startNewChatSession">新对话</el-button>
                  <el-button size="small" :loading="chatSessionsLoading" @click="refreshChatSessions">刷新历史</el-button>
                  <span class="chat-history-tip">
                    {{ currentUser ? '登录后自动保存 AI 对话' : '登录后可保存和恢复对话' }}
                  </span>
                </div>
                <div v-if="currentUser && chatSessions.length > 0" class="chat-session-list">
                  <button
                    v-for="session in chatSessions"
                    :key="session.id"
                    class="chat-session-item"
                    :class="{ 'chat-session-item--active': session.id === chatSessionId }"
                    type="button"
                    @click="restoreChatSession(session.id)"
                  >
                    <span>{{ session.title || '新的 AI 对话' }}</span>
                    <small>{{ formatTime(session.updatedAt) }}</small>
                    <el-button
                      class="chat-session-delete"
                      size="small"
                      text
                      type="danger"
                      @click.stop="removeChatSession(session.id)"
                    >
                      删除
                    </el-button>
                  </button>
                </div>
              </div>
              <div class="chat-messages">
                <div v-if="chatMessages.length === 0" class="empty-card">
                  <strong>问问 CareerPilot</strong>
                  <span>可以追问简历优化、面试回答、项目描述和岗位准备。</span>
                </div>
                <article
                  v-for="(message, index) in chatMessages"
                  :key="index"
                  class="chat-bubble"
                  :class="`chat-bubble--${message.role}`"
                >
                  <p>{{ message.content }}</p>
                </article>
              </div>
              <div class="chat-input">
                <el-input
                  v-model="chatInput"
                  type="textarea"
                  :rows="3"
                  resize="none"
                  placeholder="例如：帮我把 CareerPilot 项目经历写得更适合 Java 后端实习"
                  @keydown.enter.exact.prevent="handleChat"
                />
                <el-button class="send-button" type="primary" :loading="chatting" @click="handleChat">发送</el-button>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </aside>
    </section>

    <el-dialog v-model="settingsVisible" title="AI 设置" width="520px">
      <el-alert
        :title="settingsTip"
        type="info"
        :closable="false"
      />
      <el-form label-position="top" class="settings-form">
        <el-form-item label="AI_API_KEY">
          <el-input v-model="aiConfigDraft.apiKey" type="password" show-password placeholder="sk-..." />
        </el-form-item>
        <el-form-item label="AI_BASE_URL">
          <el-input v-model="aiConfigDraft.baseUrl" placeholder="https://api.okinto.com/v1" />
        </el-form-item>
        <el-form-item label="AI_MODEL">
          <el-input v-model="aiConfigDraft.model" placeholder="gpt-5.5" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="clearAiConfig">清空配置</el-button>
        <el-button type="primary" @click="saveAiConfig">保存配置</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="authVisible" width="480px" class="auth-dialog">
      <template #header>
        <div class="auth-header">
          <div class="auth-logo">CP</div>
          <div>
            <h2>{{ authTitle }}</h2>
            <p>{{ authSubtitle }}</p>
          </div>
        </div>
      </template>
      <el-alert
        class="auth-tip"
        :title="authTip"
        type="info"
        :closable="false"
      />
      <el-form label-position="top" class="settings-form auth-form" @submit.prevent>
        <el-form-item v-if="authMode === 'login'" label="用户名 / 手机号">
          <el-input v-model="authForm.identifier" size="large" placeholder="输入用户名或手机号" maxlength="20" />
        </el-form-item>
        <el-form-item v-if="authMode === 'register'" label="用户名">
          <el-input v-model="authForm.username" size="large" placeholder="3-20 位用户名" maxlength="20" />
        </el-form-item>
        <el-form-item v-if="authMode !== 'login'" label="手机号">
          <el-input v-model="authForm.phone" size="large" placeholder="请输入 11 位手机号" maxlength="11" />
        </el-form-item>
        <el-form-item v-if="authMode !== 'login'" label="验证码">
          <div class="code-row">
            <el-input v-model="authForm.code" size="large" placeholder="输入 4 位验证码" maxlength="4" />
            <el-button size="large" :disabled="codeCountdown > 0" :loading="sendingCode" @click="sendCode">
              {{ codeButtonText }}
            </el-button>
            <el-tag v-if="visibleCode" type="success" effect="light">{{ visibleCode }}</el-tag>
          </div>
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="authForm.password" size="large" type="password" show-password :placeholder="authMode === 'reset' ? '输入新密码' : '至少 6 位密码'" maxlength="32" />
        </el-form-item>
        <el-form-item v-if="authMode !== 'login'" label="确认密码">
          <el-input v-model="authForm.confirmPassword" size="large" type="password" show-password placeholder="再次输入密码" maxlength="32" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="auth-footer">
          <el-button v-if="authMode === 'login'" text class="auth-link" @click="switchAuthMode('reset')">忘记密码</el-button>
          <span v-else></span>
          <div class="auth-actions">
            <el-button size="large" @click="switchAuthMode(authMode === 'login' ? 'register' : 'login')">
              {{ authMode === 'login' ? '去注册' : '去登录' }}
            </el-button>
            <el-button size="large" type="primary" :loading="authLoading" @click="submitAuth">
              {{ authSubmitText }}
            </el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="resumeBuilderVisible" title="模板化简历编辑器" width="1180px" class="resume-builder-dialog">
      <div class="resume-studio">
        <aside class="resume-editor-panel">
          <div class="template-strip">
            <button
              v-for="template in resumeTemplates"
              :key="template.id"
              class="template-card"
              :class="{ 'template-card--active': resumeTemplateId === template.id }"
              type="button"
              @click="resumeTemplateId = template.id"
            >
              <span :style="{ background: template.accent }"></span>
              <strong>{{ template.name }}</strong>
              <small>{{ template.description }}</small>
            </button>
          </div>

          <div class="builder-feature-grid">
            <section class="builder-feature-card">
              <div class="builder-feature-card__head">
                <strong>主题与字体</strong>
                <span>自定义颜色方案和专业字体</span>
              </div>
              <div class="builder-mini-grid">
                <el-select v-model="resumeThemePresetId" size="small" @change="applyThemePreset">
                  <el-option
                    v-for="preset in resumeThemePresets"
                    :key="preset.id"
                    :label="preset.name"
                    :value="preset.id"
                  />
                </el-select>
                <el-color-picker v-model="resumeThemeColor" show-alpha />
              </div>
              <el-select v-model="resumeFontFamily" size="small" class="builder-font-select">
                <el-option
                  v-for="font in resumeFontOptions"
                  :key="font.value"
                  :label="font.label"
                  :value="font.value"
                />
              </el-select>
            </section>

            <section class="builder-feature-card">
              <div class="builder-feature-card__head">
                <strong>照片 / 头像</strong>
                <span>本地上传预览，导出时保留</span>
              </div>
              <div class="photo-preview-box">
                <div class="photo-preview-frame" :class="`photo-preview-frame--${resumeProfile.photoShape}`" @click="openResumePhotoPicker">
                  <img v-if="resumeProfile.photoVisible && resumeProfile.photoUrl" :src="resumeProfile.photoUrl" alt="照片预览" />
                  <div v-else class="photo-preview-placeholder">{{ resumePhotoFallbackText }}</div>
                </div>
                <div class="photo-preview-text">
                  <p>支持 PNG / JPG / WEBP</p>
                  <p>照片不上传服务器，只保存在当前浏览器。</p>
                </div>
              </div>
              <div class="photo-actions">
                <input ref="resumePhotoInputRef" class="photo-input" type="file" accept="image/png,image/jpeg,image/webp" @change="handleResumePhotoPick" />
                <el-button type="primary" plain @click="openResumePhotoPicker">上传照片</el-button>
                <el-switch v-model="resumeProfile.photoVisible" active-text="显示照片" inactive-text="隐藏照片" />
                <el-radio-group v-model="resumeProfile.photoShape" size="small">
                  <el-radio-button label="portrait">证件照</el-radio-button>
                  <el-radio-button label="square">方形头像</el-radio-button>
                </el-radio-group>
                <el-button plain @click="clearResumePhoto">清除照片</el-button>
              </div>
            </section>

            <section class="builder-feature-card builder-feature-card--sections">
              <div class="builder-feature-card__head">
                <strong>模块标题</strong>
                <span>可在这里直接改标题，也可在简历上直接编辑</span>
              </div>
              <div v-for="(section, index) in resumeSections" :key="section.key" class="section-control">
                <el-input v-model="section.label" size="small" />
                <el-switch v-model="section.visible" />
                <el-button size="small" plain @click="moveSectionUp(index)">↑</el-button>
                <el-button size="small" plain @click="moveSectionDown(index)">↓</el-button>
                <el-button size="small" plain @click="resetSectionLabel(section.key)">恢复</el-button>
              </div>
            </section>
          </div>

          <el-form label-position="top" class="resume-builder-form">
            <div class="builder-two-cols">
              <el-form-item label="姓名">
                <el-input v-model="resumeProfile.name" placeholder="例如：张三" />
              </el-form-item>
              <el-form-item label="求职意向">
                <el-input v-model="resumeProfile.targetRole" placeholder="Java 后端开发实习生" />
              </el-form-item>
            </div>
            <div class="builder-three-cols">
              <el-form-item label="手机号">
                <el-input v-model="resumeProfile.phone" placeholder="13800000000" />
              </el-form-item>
              <el-form-item label="邮箱">
                <el-input v-model="resumeProfile.email" placeholder="name@example.com" />
              </el-form-item>
              <el-form-item label="所在地">
                <el-input v-model="resumeProfile.location" placeholder="上海" />
              </el-form-item>
            </div>
            <el-form-item label="教育经历">
              <el-input v-model="resumeProfile.education" type="textarea" :rows="3" resize="none" placeholder="学校、专业、时间、主修课程、绩点或荣誉" />
            </el-form-item>
            <el-form-item label="技能栈">
              <el-input v-model="resumeProfile.skills" type="textarea" :rows="4" resize="none" placeholder="Java、Spring Boot、MySQL、RESTful API、Vue3、AI API 调用..." />
            </el-form-item>
            <el-form-item label="项目经历">
              <el-input v-model="resumeProfile.projects" type="textarea" :rows="5" resize="none" placeholder="项目名称、技术栈、职责、结果" />
            </el-form-item>
            <el-form-item label="实习 / 校园经历">
              <el-input v-model="resumeProfile.experience" type="textarea" :rows="4" resize="none" placeholder="竞赛、社团、实习、兼职中能体现沟通、责任心的经历" />
            </el-form-item>
            <el-form-item label="自我评价">
              <el-input v-model="resumeProfile.evaluation" type="textarea" :rows="3" resize="none" placeholder="技术导向、学习能力、项目经验、求职态度" />
            </el-form-item>
            <el-form-item label="其他 / 原始内容">
              <el-input v-model="resumeProfile.extra" type="textarea" :rows="3" resize="none" placeholder="上传简历中暂未识别的内容会放在这里" />
            </el-form-item>
          </el-form>
        </aside>

        <section class="visual-resume-preview">
          <div class="visual-preview-toolbar">
            <div>
              <strong>A4 简历编辑预览</strong>
              <span>{{ currentResumeTemplate.name }} · 点击简历文字可直接修改</span>
            </div>
            <div class="resume-zoom-controls">
              <el-button size="small" plain @click="zoomResumeOut">-</el-button>
              <span>{{ resumeZoomPercent }}</span>
              <el-button size="small" plain @click="zoomResumeIn">+</el-button>
              <el-button size="small" plain @click="fitResumeWidth">适应宽度</el-button>
              <el-button size="small" plain @click="resetResumeZoom">100%</el-button>
              <el-tag v-if="builtResumeAiEnhanced" type="success" effect="plain">AI 已优化</el-tag>
              <el-tag v-else type="info" effect="plain">可直接编辑</el-tag>
            </div>
          </div>
          <div ref="resumePreviewScrollRef" class="a4-scroll">
            <div class="a4-stage" :style="a4StageStyle">
              <article
                ref="resumePreviewRef"
                class="a4-page editable-a4-page"
                :style="a4PageStyle"
                @blur.capture="syncResumePreviewEdits"
                v-html="resumePreviewHtml"
              ></article>
            </div>
          </div>
        </section>
      </div>
      <template #footer>
        <div class="resume-builder-footer">
          <div class="resume-builder-actions">
            <el-button @click="importCurrentResumeToBuilder" :disabled="!resumeText.trim()">导入当前简历</el-button>
            <el-button @click="applyBuiltResume">填入左侧简历文本</el-button>
            <el-button type="primary" :loading="resumeBuilding" @click="handleBuildResume">AI 优化内容</el-button>
          </div>
          <div class="resume-export-actions">
            <el-button @click="downloadMarkdown">Markdown</el-button>
            <el-button @click="downloadHtml">HTML</el-button>
            <el-button @click="downloadWord">Word</el-button>
            <el-button @click="downloadJson">JSON</el-button>
            <el-button type="success" plain @click="printPdf">PDF</el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <el-drawer v-model="recordsVisible" title="最近记录" direction="rtl" size="420px">
      <div v-if="records.length === 0" class="empty-card">
        <strong>暂无记录</strong>
        <span>完成一次分析后会自动保存到这里。</span>
      </div>
      <div v-else class="record-list">
        <article v-for="record in records" :key="record.id" class="record-card">
          <div class="record-card__head">
            <button class="record-title" type="button" @click="restoreRecord(record.id)">
              {{ record.resumeName }}
            </button>
            <span>{{ formatTime(record.createdAt) }}</span>
          </div>
          <el-tag type="primary" effect="light">{{ record.matchScore }} 分</el-tag>
          <p>{{ record.summary }}</p>
          <div class="record-card__actions">
            <el-button size="small" type="primary" plain :loading="recordLoadingId === record.id" @click="restoreRecord(record.id)">
              查看详情
            </el-button>
            <el-button size="small" type="danger" plain :loading="recordDeletingId === record.id" @click="removeRecord(record)">
              删除
            </el-button>
          </div>
        </article>
      </div>
    </el-drawer>
  </main>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ResultList from './components/ResultList.vue'
import {
  AUTH_TOKEN_KEY,
  analyzeResume,
  buildResume,
  deleteChatSession,
  deleteRecord,
  generateQuestions,
  loadChatSessionDetail,
  loadChatSessions,
  loadCurrentUser,
  loadAiStatus,
  loadRecordDetail,
  loadRecords,
  login,
  parseResume,
  register,
  resetPassword,
  sendAuthCode,
  sendChatMessage
} from './api/client'

const AI_CONFIG_KEY = 'careerpilot_ai_config'

const PanelHeader = defineComponent({
  props: {
    title: { type: String, required: true },
    subtitle: { type: String, required: true }
  },
  setup(props) {
    return () => h('div', { class: 'panel-header' }, [
      h('div', [
        h('h2', props.title),
        h('p', props.subtitle)
      ])
    ])
  }
})

const resumeName = ref('')
const resumeText = ref('')
const jobDescription = ref('')
const analysis = ref(null)
const questions = ref([])
const records = ref([])
const interviewFeedback = ref('')
const analyzing = ref(false)
const interviewing = ref(false)
const aiStatus = ref(null)
const recordsVisible = ref(false)
const settingsVisible = ref(false)
const authVisible = ref(false)
const resumeBuilderVisible = ref(false)
const authMode = ref('login')
const authLoading = ref(false)
const sendingCode = ref(false)
const codeCountdown = ref(0)
const visibleCode = ref('')
let codeTimer = null
const authForm = ref({
  identifier: '',
  username: '',
  phone: '',
  code: '',
  password: '',
  confirmPassword: ''
})
const currentUser = ref(null)
const rightTab = ref('interview')
const openSections = ref(['strengths', 'gaps', 'suggestions'])
const chatInput = ref('')
const chatting = ref(false)
const chatMessages = ref([])
const chatSessionId = ref(null)
const chatSessions = ref([])
const chatSessionsLoading = ref(false)
const recordLoadingId = ref(null)
const recordDeletingId = ref(null)
const resumeBuilding = ref(false)
const builtResumeMarkdown = ref('')
const builtResumeAiEnhanced = ref(false)
const editableResumeMarkdown = ref('')
const resumePreviewRef = ref(null)
const resumePreviewScrollRef = ref(null)
const resumePhotoInputRef = ref(null)
const resumeZoom = ref(0.72)
const resumeTemplateId = ref('techBlue')
const resumeTemplates = [
  { id: 'techBlue', name: '蓝色科技风', description: '后端 / AI 应用', accent: '#2f7df6' },
  { id: 'purpleSidebar', name: '紫色侧栏风', description: '醒目有设计感', accent: '#7c3aed' },
  { id: 'freshGreen', name: '绿色清新风', description: '校园招聘', accent: '#10b981' },
  { id: 'classic', name: '黑白极简风', description: '正式投递', accent: '#111827' }
]
resumeTemplates.push(
  { id: 'navyBusiness', name: '深蓝商务风', description: '稳重后端岗', accent: '#1d4ed8' },
  { id: 'orangeModern', name: '橙色活力风', description: '年轻有冲击力', accent: '#f97316' },
  { id: 'slateCard', name: '卡片分区风', description: '模块清晰', accent: '#475569' },
  { id: 'redLine', name: '红黑线条风', description: '正式且醒目', accent: '#dc2626' }
)
const resumeSectionDefaults = {
  education: '教育背景',
  skills: '技能栈',
  projects: '项目经历',
  experience: '实习 / 校园经历',
  evaluation: '自我评价',
  extra: '其他信息'
}
const resumeThemePresets = [
  { id: 'ocean', name: '海蓝专业', accent: '#2f7df6' },
  { id: 'violet', name: '沉稳紫', accent: '#7c3aed' },
  { id: 'mint', name: '清新绿', accent: '#10b981' },
  { id: 'amber', name: '暖橙', accent: '#f97316' },
  { id: 'slate', name: '石墨灰', accent: '#475569' },
  { id: 'crimson', name: '红黑风', accent: '#dc2626' }
]
const resumeFontOptions = [
  { label: '系统无衬线', value: '-apple-system, BlinkMacSystemFont, "Segoe UI", "Microsoft YaHei", sans-serif' },
  { label: '微软雅黑', value: '"Microsoft YaHei", "PingFang SC", sans-serif' },
  { label: '苹方', value: '"PingFang SC", "Microsoft YaHei", sans-serif' },
  { label: '思源黑体', value: '"Source Han Sans SC", "Noto Sans SC", "Microsoft YaHei", sans-serif' },
  { label: 'HarmonyOS Sans', value: '"HarmonyOS Sans", "Microsoft YaHei", sans-serif' },
  { label: '商务衬线', value: 'Georgia, "Times New Roman", serif' }
]
const resumeThemePresetId = ref('ocean')
const resumeThemeColor = ref('#2f7df6')
const resumeFontFamily = ref('"Microsoft YaHei", "PingFang SC", sans-serif')
const resumeProfile = ref({
  name: '',
  targetRole: 'Java 后端开发实习生',
  phone: '',
  email: '',
  location: '',
  photoUrl: '',
  photoVisible: true,
  photoShape: 'portrait',
  education: '',
  skills: 'Java、Spring Boot、MySQL、RESTful API、Vue3、AI API 调用',
  projects: 'CareerPilot AI 简历分析与模拟面试平台：基于 Spring Boot + Vue3 开发，支持简历解析、岗位匹配分析、模拟面试题生成、AI 对话和历史记录管理。',
  experience: '',
  evaluation: '熟悉 Java / Spring Boot 后端开发，具备完整 AI Web 项目开发经验，了解前后端联调、接口设计和数据持久化。',
  extra: ''
})
const resumeSections = ref([
  { key: 'education', label: '教育背景', visible: true },
  { key: 'skills', label: '技能栈', visible: true },
  { key: 'projects', label: '项目经历', visible: true },
  { key: 'experience', label: '实习 / 校园经历', visible: true },
  { key: 'evaluation', label: '自我评价', visible: true },
  { key: 'extra', label: '其他信息', visible: false }
])
const aiConfig = ref(loadLocalAiConfig())
const aiConfigDraft = ref({ ...aiConfig.value })

const localAiConfigured = computed(() => Boolean(aiConfig.value.apiKey && aiConfig.value.baseUrl && aiConfig.value.model))
const aiStatusLabel = computed(() => {
  if (localAiConfigured.value) {
    return `AI 已配置：${aiConfig.value.model}`
  }
  if (aiStatus.value?.configured) {
    return `AI 默认配置：${aiStatus.value.model || '后端模型'}`
  }
  return 'AI 未配置'
})
const aiStatusButtonType = computed(() => {
  if (localAiConfigured.value) {
    return 'primary'
  }
  if (aiStatus.value?.configured) {
    return 'success'
  }
  return 'warning'
})
const settingsTip = computed(() => {
  if (localAiConfigured.value) {
    return `当前使用你的前端 AI 配置：${aiConfig.value.model}。配置保存在当前浏览器，请不要在公共电脑保存 Key。`
  }
  if (aiStatus.value?.configured) {
    return `当前未填写个人 AI 配置，简历分析会使用后端默认模型：${aiStatus.value.model || '后端模型'}；AI 对话需要填写下面三项。`
  }
  return '当前没有可用 AI 配置。请填写 API Key、Base URL 和模型名后使用 AI 分析与对话。'
})
const authTitle = computed(() => {
  if (authMode.value === 'login') return '登录 CareerPilot'
  if (authMode.value === 'register') return '注册 CareerPilot'
  return '忘记密码'
})
const authSubtitle = computed(() => {
  if (authMode.value === 'login') return '继续管理你的简历分析记录'
  if (authMode.value === 'register') return '创建账号，保存专属求职进度'
  return '用手机号验证码重置登录密码'
})
const authTip = computed(() => {
  if (authMode.value === 'login') {
    return '登录后，分析记录会保存到你的账号下；不登录也可以继续匿名体验。'
  }
  if (authMode.value === 'register') {
    return '注册需要手机号验证码。当前为演示模式，验证码会直接显示在发送按钮旁边。'
  }
  return '忘记密码时，使用手机号验证码重置密码。验证码 5 分钟内有效。'
})
const authSubmitText = computed(() => {
  if (authMode.value === 'login') return '登录'
  if (authMode.value === 'register') return '注册并登录'
  return '修改密码'
})
const codeButtonText = computed(() => {
  if (codeCountdown.value > 0) {
    return `已发送 ${codeCountdown.value}s`
  }
  return visibleCode.value ? '重新发送' : '发送验证码'
})

const scoreLabel = computed(() => {
  const score = analysis.value?.matchScore || 0
  if (score >= 85) return '强匹配'
  if (score >= 70) return '较匹配'
  if (score >= 50) return '待优化'
  return '差距较大'
})

const scoreTagType = computed(() => {
  const score = analysis.value?.matchScore || 0
  if (score >= 85) return 'success'
  if (score >= 70) return 'primary'
  if (score >= 50) return 'warning'
  return 'danger'
})

const prioritySuggestions = computed(() => (analysis.value?.suggestions || []).slice(0, 3))
const currentResumeTemplate = computed(() => resumeTemplates.find((template) => template.id === resumeTemplateId.value) || resumeTemplates[0])
const currentResumeAccent = computed(() => resumeThemeColor.value || currentResumeTemplate.value.accent)
const visibleResumeSections = computed(() => resumeSections.value.filter((section) => section.visible && sectionContent(section.key).trim()))
const resumePreviewHtml = computed(() => renderResumeHtml())
const resumeZoomPercent = computed(() => `${Math.round(resumeZoom.value * 100)}%`)
const a4StageStyle = computed(() => ({
  width: `${Math.ceil(794 * resumeZoom.value)}px`,
  minHeight: `${Math.ceil(1123 * resumeZoom.value)}px`
}))
const a4PageStyle = computed(() => ({
  transform: `scale(${resumeZoom.value})`
}))

watch(settingsVisible, (visible) => {
  if (visible) {
    aiConfigDraft.value = { ...aiConfig.value }
  }
})

watch(resumeThemePresetId, (presetId) => {
  applyThemePreset(presetId)
}, { immediate: true })

async function handleFileChange(uploadFile) {
  try {
    const response = await parseResume(uploadFile.raw)
    resumeName.value = response.data.fileName
    resumeText.value = response.data.text
    ElMessage.success('简历解析完成')
  } catch (error) {
    ElMessage.error(error.message || '简历解析失败')
  }
}

async function handleAnalyze() {
  if (!resumeText.value.trim() || !jobDescription.value.trim()) {
    ElMessage.warning('请先填写简历文本和岗位 JD')
    return
  }

  analyzing.value = true
  try {
    const response = await analyzeResume({
      resumeName: resumeName.value || '手动输入简历',
      resumeText: resumeText.value,
      jobDescription: jobDescription.value,
      aiConfig: runtimeAiConfig()
    })
    analysis.value = response.data
    questions.value = response.data.interviewQuestions || []
    await refreshRecords()
    ElMessage.success('分析完成')
  } catch (error) {
    ElMessage.error(error.message || '分析失败，请检查后端服务')
  } finally {
    analyzing.value = false
  }
}

async function handleInterview() {
  if (!resumeText.value.trim() || !jobDescription.value.trim()) {
    ElMessage.warning('请先填写简历文本和岗位 JD')
    return
  }

  interviewing.value = true
  try {
    const response = await generateQuestions({
      resumeText: resumeText.value,
      jobDescription: jobDescription.value,
      answer: '',
      aiConfig: runtimeAiConfig()
    })
    questions.value = response.data.questions || []
    interviewFeedback.value = response.data.feedback
    rightTab.value = 'interview'
    ElMessage.success('面试题已生成')
  } catch (error) {
    ElMessage.error(error.message || '生成面试题失败')
  } finally {
    interviewing.value = false
  }
}

async function handleChat() {
  const content = chatInput.value.trim()
  if (!content) {
    return
  }
  if (!localAiConfigured.value) {
    ElMessage.warning('请先在右上角 AI 设置中填写 API Key、Base URL 和模型名')
    settingsVisible.value = true
    return
  }

  const nextMessages = [...chatMessages.value, { role: 'user', content }]
  chatMessages.value = nextMessages
  chatInput.value = ''
  chatting.value = true

  try {
    const response = await sendChatMessage({
      resumeText: resumeText.value,
      jobDescription: jobDescription.value,
      sessionId: chatSessionId.value,
      messages: nextMessages,
      aiConfig: runtimeAiConfig()
    })
    if (response.data.sessionId) {
      chatSessionId.value = response.data.sessionId
    }
    chatMessages.value.push({ role: 'assistant', content: response.data.answer })
    await refreshChatSessions(false)
  } catch (error) {
    ElMessage.error(error.message || 'AI 对话失败')
  } finally {
    chatting.value = false
  }
}

function startNewChatSession() {
  chatSessionId.value = null
  chatMessages.value = []
  chatInput.value = ''
}

async function refreshChatSessions(showError = true) {
  if (!currentUser.value) {
    chatSessions.value = []
    return
  }
  chatSessionsLoading.value = true
  try {
    const response = await loadChatSessions()
    chatSessions.value = response.data || []
  } catch (error) {
    if (showError) {
      ElMessage.error(error.message || '加载对话历史失败')
    }
  } finally {
    chatSessionsLoading.value = false
  }
}

async function restoreChatSession(id) {
  try {
    const response = await loadChatSessionDetail(id)
    const session = response.data
    chatSessionId.value = session.id
    resumeText.value = session.resumeText || resumeText.value
    jobDescription.value = session.jobDescription || jobDescription.value
    chatMessages.value = (session.messages || []).map((message) => ({
      role: message.role,
      content: message.content
    }))
    rightTab.value = 'chat'
    ElMessage.success('已恢复 AI 对话')
  } catch (error) {
    ElMessage.error(error.message || '恢复对话失败')
  }
}

async function removeChatSession(id) {
  try {
    await ElMessageBox.confirm('确定删除这段 AI 对话历史吗？', '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteChatSession(id)
    if (chatSessionId.value === id) {
      startNewChatSession()
    }
    await refreshChatSessions(false)
    ElMessage.success('AI 对话已删除')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除对话失败')
    }
  }
}

async function handleBuildResume() {
  if (!resumeProfile.value.name.trim() || !resumeProfile.value.targetRole.trim()) {
    ElMessage.warning('请至少填写姓名和求职意向')
    return
  }

  resumeBuilding.value = true
  try {
    const response = await buildResume({
      name: resumeProfile.value.name,
      targetRole: resumeProfile.value.targetRole,
      education: resumeProfile.value.education,
      skills: resumeProfile.value.skills,
      projects: resumeProfile.value.projects,
      experience: [resumeProfile.value.experience, resumeProfile.value.evaluation, resumeProfile.value.extra].filter(Boolean).join('\n\n'),
      aiConfig: runtimeAiConfig()
    })
    builtResumeMarkdown.value = response.data.markdown
    editableResumeMarkdown.value = response.data.markdown
    builtResumeAiEnhanced.value = response.data.aiEnhanced
    ElMessage.success(response.data.aiEnhanced ? 'AI 已生成简历' : '已生成本地模板简历')
  } catch (error) {
    ElMessage.error(error.message || '生成简历失败')
  } finally {
    resumeBuilding.value = false
  }
}

function applyBuiltResume() {
  syncResumePreviewEdits()
  resumeName.value = `${resumeProfile.value.name || '我的'}-模板简历`
  resumeText.value = buildResumeMarkdownFromProfile()
  resumeBuilderVisible.value = false
  ElMessage.success('已填入左侧简历文本')
}

function importCurrentResumeToBuilder() {
  if (!resumeText.value.trim()) {
    ElMessage.warning('当前没有可导入的简历文本')
    return
  }
  const parsed = parseResumeTextToProfile(resumeText.value)
  resumeProfile.value = {
    ...resumeProfile.value,
    ...parsed
  }
  editableResumeMarkdown.value = buildResumeMarkdownFromProfile()
  builtResumeMarkdown.value = editableResumeMarkdown.value
  builtResumeAiEnhanced.value = false
  ElMessage.success('已导入当前简历，可在字段中继续修改')
}

function downloadMarkdown() {
  syncResumePreviewEdits()
  downloadBlob(`${resumeFileBaseName()}.md`, buildResumeMarkdownFromProfile(), 'text/markdown;charset=utf-8')
}

function downloadHtml() {
  syncResumePreviewEdits()
  downloadBlob(`${resumeFileBaseName()}.html`, buildResumeHtmlDocument(), 'text/html;charset=utf-8')
}

function downloadWord() {
  syncResumePreviewEdits()
  downloadBlob(`${resumeFileBaseName()}-WPS可编辑版.doc`, buildEditableWordDocument(), 'application/msword;charset=utf-8')
}

function downloadJson() {
  syncResumePreviewEdits()
  const payload = {
    exportedAt: new Date().toISOString(),
    resumeName: resumeFileBaseName(),
    templateId: resumeTemplateId.value,
    theme: {
      presetId: resumeThemePresetId.value,
      accent: currentResumeAccent.value,
      fontFamily: resumeFontFamily.value
    },
    profile: resumeProfile.value,
    sections: resumeSections.value,
    markdown: buildResumeMarkdownFromProfile()
  }
  downloadBlob(`${resumeFileBaseName()}.json`, JSON.stringify(payload, null, 2), 'application/json;charset=utf-8')
}

function printPdf() {
  syncResumePreviewEdits()
  const printWindow = window.open('', '_blank')
  if (!printWindow) {
    ElMessage.warning('浏览器拦截了打印窗口，请允许弹窗后重试')
    return
  }
  printWindow.document.open()
  printWindow.document.write(buildResumeHtmlDocument())
  printWindow.document.close()
  printWindow.focus()
  window.setTimeout(() => {
    printWindow.print()
  }, 300)
}

function zoomResumeIn() {
  resumeZoom.value = clampZoom(resumeZoom.value + 0.05)
}

function zoomResumeOut() {
  resumeZoom.value = clampZoom(resumeZoom.value - 0.05)
}

function resetResumeZoom() {
  resumeZoom.value = 1
}

function fitResumeWidth() {
  const width = resumePreviewScrollRef.value?.clientWidth || 794
  resumeZoom.value = clampZoom((width - 44) / 794)
}

function clampZoom(value) {
  return Math.min(1.2, Math.max(0.45, Number(value.toFixed(2))))
}

function syncResumePreviewEdits() {
  const root = resumePreviewRef.value
  if (!root) return

  root.querySelectorAll('[data-edit-field]').forEach((element) => {
    const field = element.getAttribute('data-edit-field')
    if (field && Object.prototype.hasOwnProperty.call(resumeProfile.value, field)) {
      resumeProfile.value[field] = cleanEditableField(field, element.innerText)
    }
  })

  root.querySelectorAll('[data-edit-section-title]').forEach((element) => {
    const sectionKey = element.getAttribute('data-edit-section-title')
    if (sectionKey) {
      setResumeSectionLabel(sectionKey, element.innerText)
    }
  })

  root.querySelectorAll('[data-edit-section]').forEach((element) => {
    const section = element.getAttribute('data-edit-section')
    if (section && Object.prototype.hasOwnProperty.call(resumeProfile.value, section)) {
      resumeProfile.value[section] = element.innerText.trim()
    }
  })

  editableResumeMarkdown.value = buildResumeMarkdownFromProfile()
}

function cleanEditableField(field, value) {
  const text = value.trim()
  if (field === 'phone') return text.replace(/^电话[：:]\s*/, '')
  if (field === 'email') return text.replace(/^邮箱[：:]\s*/, '')
  if (field === 'location') return text.replace(/^所在地[：:]\s*/, '')
  return text
}

function setResumeSectionLabel(sectionKey, value) {
  const section = resumeSections.value.find((item) => item.key === sectionKey)
  if (!section) return
  const text = value.trim()
  section.label = text || resumeSectionDefaults[sectionKey] || section.label
}

function resetSectionLabel(sectionKey) {
  setResumeSectionLabel(sectionKey, resumeSectionDefaults[sectionKey] || '')
}

function applyThemePreset(presetId) {
  const preset = resumeThemePresets.find((item) => item.id === presetId)
  if (preset) {
    resumeThemeColor.value = preset.accent
  }
}

function openResumePhotoPicker() {
  resumePhotoInputRef.value?.click()
}

function handleResumePhotoPick(event) {
  const file = event?.target?.files?.[0]
  if (!file || !file.type.startsWith('image/')) {
    ElMessage.warning('请选择 PNG / JPG / WEBP 图片')
    return
  }

  const reader = new FileReader()
  reader.onload = () => {
    resumeProfile.value.photoUrl = String(reader.result || '')
    resumeProfile.value.photoVisible = true
  }
  reader.onerror = () => ElMessage.error('照片读取失败')
  reader.readAsDataURL(file)
  event.target.value = ''
}

function clearResumePhoto() {
  resumeProfile.value.photoUrl = ''
}

const resumePhotoFallbackText = computed(() => {
  const name = resumeProfile.value.name.trim()
  if (name) {
    return name.slice(0, 2)
  }
  return '照片'
})

function moveSectionUp(index) {
  if (index <= 0) return
  const sections = [...resumeSections.value]
  const current = sections[index]
  sections[index] = sections[index - 1]
  sections[index - 1] = current
  resumeSections.value = sections
}

function moveSectionDown(index) {
  if (index >= resumeSections.value.length - 1) return
  const sections = [...resumeSections.value]
  const current = sections[index]
  sections[index] = sections[index + 1]
  sections[index + 1] = current
  resumeSections.value = sections
}

function parseResumeTextToProfile(text) {
  const normalized = text.replace(/\r/g, '').trim()
  const lines = normalized.split('\n').map((line) => line.trim()).filter(Boolean)
  const lowerText = normalized.toLowerCase()
  return {
    name: resumeProfile.value.name || inferName(lines),
    targetRole: inferTargetRole(normalized) || resumeProfile.value.targetRole,
    phone: normalized.match(/1\d{10}/)?.[0] || resumeProfile.value.phone,
    email: normalized.match(/[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/i)?.[0] || resumeProfile.value.email,
    location: resumeProfile.value.location,
    education: extractSection(normalized, ['教育背景', '教育经历', 'education']) || resumeProfile.value.education,
    skills: extractSection(normalized, ['技能栈', '专业技能', '技能', 'skills']) || resumeProfile.value.skills,
    projects: extractSection(normalized, ['项目经历', '项目经验', 'projects']) || resumeProfile.value.projects,
    experience: extractSection(normalized, ['实习经历', '校园经历', '社会经历', '工作经历', 'experience']) || resumeProfile.value.experience,
    evaluation: extractSection(normalized, ['自我评价', '个人评价', 'self-evaluation']) || resumeProfile.value.evaluation,
    extra: lowerText.includes('education') || lowerText.includes('experience') ? normalized : resumeProfile.value.extra || normalized
  }
}

function inferName(lines) {
  const candidate = lines.find((line) => line.length >= 2 && line.length <= 12 && !/[：:]/.test(line))
  return candidate || ''
}

function inferTargetRole(text) {
  const match = text.match(/(?:求职意向|应聘意向|目标岗位)[：:\s]*(.+)/)
  return match?.[1]?.trim() || ''
}

function extractSection(text, titles) {
  const lines = text.split('\n')
  const titleIndex = lines.findIndex((line) => titles.some((title) => line.toLowerCase().includes(title.toLowerCase())))
  if (titleIndex < 0) return ''

  const stopWords = ['教育背景', '教育经历', '技能栈', '专业技能', '技能', '项目经历', '项目经验', '实习经历', '校园经历', '社会经历', '工作经历', '自我评价', '个人评价', '个人信息', '求职意向']
  const content = []
  for (let index = titleIndex + 1; index < lines.length; index++) {
    const line = lines[index].trim()
    if (!line) continue
    if (content.length > 0 && stopWords.some((word) => line.includes(word))) {
      break
    }
    content.push(line)
  }
  return content.join('\n').trim()
}

async function refreshRecords() {
  const response = await loadRecords()
  records.value = response.data
}

async function restoreRecord(id) {
  recordLoadingId.value = id
  try {
    const response = await loadRecordDetail(id)
    const record = response.data
    resumeName.value = record.resumeName || ''
    resumeText.value = record.resumeText || ''
    jobDescription.value = record.jobDescription || ''
    analysis.value = record.result
    questions.value = record.result?.interviewQuestions || []
    interviewFeedback.value = ''
    recordsVisible.value = false
    rightTab.value = questions.value.length ? 'interview' : rightTab.value
    ElMessage.success('已恢复历史分析')
  } catch (error) {
    ElMessage.error(error.message || '加载历史记录失败')
  } finally {
    recordLoadingId.value = null
  }
}

async function removeRecord(record) {
  try {
    await ElMessageBox.confirm(
      `确定删除「${record.resumeName}」这条分析记录吗？`,
      '删除记录',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch (error) {
    return
  }

  recordDeletingId.value = record.id
  try {
    await deleteRecord(record.id)
    records.value = records.value.filter((item) => item.id !== record.id)
    ElMessage.success('记录已删除')
  } catch (error) {
    ElMessage.error(error.message || '删除记录失败')
  } finally {
    recordDeletingId.value = null
  }
}

async function refreshAiStatus() {
  try {
    const response = await loadAiStatus()
    aiStatus.value = response.data
  } catch (error) {
    aiStatus.value = { configured: false, model: '未知' }
  }
}

function openAuth(mode) {
  switchAuthMode(mode)
  authVisible.value = true
}

function switchAuthMode(mode) {
  authMode.value = mode
  authForm.value = {
    identifier: '',
    username: '',
    phone: '',
    code: '',
    password: '',
    confirmPassword: ''
  }
  visibleCode.value = ''
  stopCodeCountdown()
}

async function sendCode() {
  if (!/^1\d{10}$/.test(authForm.value.phone.trim())) {
    ElMessage.warning('请先填写正确的手机号')
    return
  }

  sendingCode.value = true
  try {
    const response = await sendAuthCode({
      phone: authForm.value.phone.trim(),
      purpose: authMode.value === 'register' ? 'REGISTER' : 'RESET_PASSWORD'
    })
    visibleCode.value = response.data.code
    authForm.value.code = response.data.code
    startCodeCountdown(response.data.resendSeconds || 10)
    ElMessage.success('验证码已发送')
  } catch (error) {
    ElMessage.error(error.message || '验证码发送失败')
  } finally {
    sendingCode.value = false
  }
}

async function submitAuth() {
  if (!authForm.value.password.trim()) {
    ElMessage.warning('请填写密码')
    return
  }
  if (authMode.value === 'login' && !authForm.value.identifier.trim()) {
    ElMessage.warning('请填写用户名或手机号')
    return
  }
  if (authMode.value !== 'login' && !authForm.value.code.trim()) {
    ElMessage.warning('请填写验证码')
    return
  }
  if (authMode.value !== 'login' && authForm.value.password !== authForm.value.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }

  authLoading.value = true
  try {
    if (authMode.value === 'reset') {
      await resetPassword({
        phone: authForm.value.phone,
        code: authForm.value.code,
        password: authForm.value.password,
        confirmPassword: authForm.value.confirmPassword
      })
      ElMessage.success('密码已修改，请重新登录')
      switchAuthMode('login')
      return
    }

    const response = authMode.value === 'login'
      ? await login({
        identifier: authForm.value.identifier,
        password: authForm.value.password
      })
      : await register({
        username: authForm.value.username,
        phone: authForm.value.phone,
        code: authForm.value.code,
        password: authForm.value.password,
        confirmPassword: authForm.value.confirmPassword
      })
    localStorage.setItem(AUTH_TOKEN_KEY, response.data.token)
    currentUser.value = {
      userId: response.data.userId,
      username: response.data.username,
      phone: response.data.phone
    }
    authVisible.value = false
    await refreshRecords()
    await refreshChatSessions(false)
    ElMessage.success(authMode.value === 'login' ? '登录成功' : '注册成功，已自动登录')
  } catch (error) {
    ElMessage.error(error.message || '认证失败')
  } finally {
    authLoading.value = false
  }
}

function startCodeCountdown(seconds) {
  stopCodeCountdown()
  codeCountdown.value = seconds
  codeTimer = window.setInterval(() => {
    codeCountdown.value -= 1
    if (codeCountdown.value <= 0) {
      stopCodeCountdown()
    }
  }, 1000)
}

function stopCodeCountdown() {
  if (codeTimer) {
    window.clearInterval(codeTimer)
    codeTimer = null
  }
  codeCountdown.value = 0
}

async function refreshCurrentUser() {
  if (!localStorage.getItem(AUTH_TOKEN_KEY)) {
    currentUser.value = null
    chatSessions.value = []
    return
  }

  try {
    const response = await loadCurrentUser()
    currentUser.value = response.data
    await refreshChatSessions(false)
  } catch (error) {
    localStorage.removeItem(AUTH_TOKEN_KEY)
    currentUser.value = null
    chatSessions.value = []
  }
}

async function logout() {
  localStorage.removeItem(AUTH_TOKEN_KEY)
  currentUser.value = null
  analysis.value = null
  questions.value = []
  interviewFeedback.value = ''
  chatSessions.value = []
  startNewChatSession()
  await refreshRecords()
  ElMessage.success('已退出登录')
}

function saveAiConfig() {
  aiConfig.value = {
    apiKey: aiConfigDraft.value.apiKey.trim(),
    baseUrl: aiConfigDraft.value.baseUrl.trim(),
    model: aiConfigDraft.value.model.trim()
  }
  localStorage.setItem(AI_CONFIG_KEY, JSON.stringify(aiConfig.value))
  settingsVisible.value = false
  ElMessage.success('AI 配置已保存')
}

function clearAiConfig() {
  aiConfig.value = { apiKey: '', baseUrl: '', model: '' }
  aiConfigDraft.value = { ...aiConfig.value }
  localStorage.removeItem(AI_CONFIG_KEY)
  ElMessage.success('AI 配置已清空')
}

function runtimeAiConfig() {
  return localAiConfigured.value ? aiConfig.value : null
}

function loadLocalAiConfig() {
  try {
    return JSON.parse(localStorage.getItem(AI_CONFIG_KEY)) || { apiKey: '', baseUrl: '', model: '' }
  } catch (error) {
    return { apiKey: '', baseUrl: '', model: '' }
  }
}

function formatTime(value) {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 16)
}

function resumeFileBaseName() {
  return safeFileName(resumeProfile.value.name || resumeName.value || 'CareerPilot简历')
}

function safeFileName(value) {
  return (value || 'CareerPilot简历')
    .replace(/[\\/:*?"<>|]/g, '-')
    .replace(/\s+/g, '-')
    .replace(/-+/g, '-')
    .slice(0, 40)
}

function downloadBlob(fileName, content, mimeType) {
  const blob = new Blob(['\ufeff', content], { type: mimeType })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

function buildResumeHtmlDocument() {
  return `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8" />
  <title>${escapeHtml(resumeFileBaseName())}</title>
  <style>
    html, body, .resume-export-shell, .resume-template, .resume-template * {
      -webkit-print-color-adjust: exact !important;
      print-color-adjust: exact !important;
    }
    @page {
      size: A4;
      margin: 0;
    }
    body {
      margin: 0;
      padding: 24px;
      background: #eef2f8;
      -webkit-print-color-adjust: exact !important;
      print-color-adjust: exact !important;
    }
    @media print {
      body { padding: 0; background: #ffffff !important; }
      .resume-export-shell {
        margin: 0 !important;
        box-shadow: none !important;
        -webkit-print-color-adjust: exact !important;
        print-color-adjust: exact !important;
      }
      .resume-template,
      .resume-template * {
        -webkit-print-color-adjust: exact !important;
        print-color-adjust: exact !important;
      }
    }
  </style>
</head>
<body>
  <main class="resume-export-shell">
    ${renderResumeHtml()}
  </main>
</body>
</html>`
}

function buildEditableWordDocument() {
  const accent = currentResumeAccent.value
  const profile = resumeProfile.value
  const contactItems = [
    profile.phone ? `电话：${profile.phone}` : '',
    profile.email ? `邮箱：${profile.email}` : '',
    profile.location ? `所在地：${profile.location}` : ''
  ].filter(Boolean)
  const photoHtml = profile.photoVisible && profile.photoUrl
    ? `<td style="width:96px;text-align:right;vertical-align:top;"><img src="${escapeHtml(profile.photoUrl)}" width="86" height="${profile.photoShape === 'square' ? '86' : '108'}" style="display:block;border:2px solid #ffffff;object-fit:cover;" /></td>`
    : ''
  const sectionHtml = visibleResumeSections.value.map((section) => `
    <tr>
      <td colspan="${photoHtml ? 2 : 1}" style="padding:14px 0 4px;border-bottom:1.5pt solid ${accent};">
        <h2 style="margin:0;color:${accent};font-size:14pt;font-weight:bold;">${escapeHtml(section.label)}</h2>
      </td>
    </tr>
    <tr>
      <td colspan="${photoHtml ? 2 : 1}" style="padding:7px 0 2px;color:#263244;font-size:10.5pt;line-height:1.55;">
        ${wordSectionContent(sectionContent(section.key))}
      </td>
    </tr>
  `).join('')

  return `<!DOCTYPE html>
<html xmlns:o="urn:schemas-microsoft-com:office:office"
      xmlns:w="urn:schemas-microsoft-com:office:word"
      xmlns="http://www.w3.org/TR/REC-html40">
<head>
  <meta charset="utf-8">
  <meta name="ProgId" content="Word.Document">
  <meta name="Generator" content="CareerPilot">
  <meta name="Originator" content="CareerPilot">
  <title>${escapeHtml(resumeFileBaseName())}</title>
  <!--[if gte mso 9]>
  <xml>
    <w:WordDocument>
      <w:View>Print</w:View>
      <w:Zoom>100</w:Zoom>
      <w:DoNotOptimizeForBrowser/>
    </w:WordDocument>
  </xml>
  <![endif]-->
  <style>
    @page Section1 { size: 595.3pt 841.9pt; margin: 36pt 42pt 36pt 42pt; }
    div.Section1 { page: Section1; }
    body { margin: 0; font-family: "Microsoft YaHei", "SimSun", Arial, sans-serif; color: #172033; }
    table { border-collapse: collapse; mso-table-lspace: 0pt; mso-table-rspace: 0pt; }
    p { margin: 0 0 5px; }
    ul { margin: 0 0 0 18px; padding: 0; }
    li { margin: 0 0 4px; }
  </style>
</head>
<body>
  <div class="Section1">
    <table width="100%" cellpadding="0" cellspacing="0" style="width:100%;background:${accent};color:#ffffff;">
      <tr>
        <td style="padding:18px 20px;vertical-align:top;">
          <h1 style="margin:0 0 7px;font-size:24pt;line-height:1.1;color:#ffffff;">${escapeHtml(profile.name || '你的姓名')}</h1>
          <p style="margin:0 0 9px;font-size:12pt;font-weight:bold;color:#ffffff;">${escapeHtml(profile.targetRole || 'Java 后端开发实习生')}</p>
          <p style="margin:0;font-size:9.5pt;color:#ffffff;">${escapeHtml(contactItems.join('  ｜  '))}</p>
        </td>
        ${photoHtml}
      </tr>
    </table>
    <table width="100%" cellpadding="0" cellspacing="0" style="width:100%;">
      ${sectionHtml}
    </table>
  </div>
</body>
</html>`
}

function renderResumeHtml() {
  if (resumeTemplateId.value === 'purpleSidebar') return renderTemplatePurpleSidebar()
  if (resumeTemplateId.value === 'freshGreen') return renderTemplateFreshGreen()
  if (resumeTemplateId.value === 'classic') return renderTemplateClassic()
  if (resumeTemplateId.value === 'navyBusiness') return renderTemplateNavyBusiness()
  if (resumeTemplateId.value === 'orangeModern') return renderTemplateOrangeModern()
  if (resumeTemplateId.value === 'slateCard') return renderTemplateSlateCard()
  if (resumeTemplateId.value === 'redLine') return renderTemplateRedLine()
  return renderTemplateTechBlue()
}

function editableFieldAttrs(field) {
  return `contenteditable="true" spellcheck="false" title="点击直接编辑" data-edit-field="${field}"`
}

function editableSectionTitleAttrs(key) {
  return `contenteditable="true" spellcheck="false" title="点击直接编辑标题" data-edit-section-title="${key}"`
}

function editableSectionAttrs(key) {
  return `contenteditable="true" spellcheck="false" title="点击直接编辑这一段" data-edit-section="${key}"`
}

function renderTopRightBlock(layoutClass = '') {
  const parts = []
  if (resumeProfile.value.photoVisible) {
    parts.push(renderPhotoBlock(layoutClass))
  }
  parts.push(`<div class="resume-contact">${renderContactItems()}</div>`)
  return `<div class="resume-top-right ${layoutClass ? `resume-top-right--${layoutClass}` : ''}">${parts.join('')}</div>`
}

function renderPhotoBlock(layoutClass = '') {
  if (!resumeProfile.value.photoVisible) {
    return ''
  }
  const photoClass = `${layoutClass ? `resume-avatar--${layoutClass}` : ''} resume-avatar--${resumeProfile.value.photoShape === 'square' ? 'square' : 'portrait'}`
  if (resumeProfile.value.photoUrl) {
    return `<div class="resume-avatar ${photoClass}"><img src="${escapeHtml(resumeProfile.value.photoUrl)}" alt="照片" /></div>`
  }
  return `<div class="resume-avatar resume-avatar--placeholder ${photoClass}"><span>${escapeHtml(resumePhotoFallbackText.value)}</span><small>点击上传</small></div>`
}

function renderTemplateTechBlue() {
  return `${baseResumeStyle(currentResumeAccent.value, resumeFontFamily.value)}
    <section class="resume-template resume-template--tech">
      <header class="resume-hero">
        <div class="resume-hero__left">
          <div class="resume-hero__title">
          <h1 contenteditable="true" data-edit-field="name">${escapeHtml(resumeProfile.value.name || '你的姓名')}</h1>
          <p contenteditable="true" data-edit-field="targetRole">${escapeHtml(resumeProfile.value.targetRole || 'Java 后端开发实习生')}</p>
          </div>
          <div class="resume-hero__meta">${renderContactItems()}</div>
        </div>
        <div class="resume-hero__photo">${renderPhotoBlock('hero')}</div>
      </header>
      <div class="resume-body">${renderSections()}</div>
    </section>`
}

function renderTemplatePurpleSidebar() {
  return `${baseResumeStyle(currentResumeAccent.value, resumeFontFamily.value)}
    <section class="resume-template resume-template--purple">
      <aside class="resume-side">
        ${renderTopRightBlock('side')}
        <h1 contenteditable="true" data-edit-field="name">${escapeHtml(resumeProfile.value.name || '你的姓名')}</h1>
        <p contenteditable="true" data-edit-field="targetRole">${escapeHtml(resumeProfile.value.targetRole || 'Java 后端开发实习生')}</p>
      </aside>
      <main class="resume-main">${renderSections()}</main>
    </section>`
}

function renderTemplateFreshGreen() {
  return `${baseResumeStyle(currentResumeAccent.value, resumeFontFamily.value)}
    <section class="resume-template resume-template--fresh">
      <header class="resume-fresh-head">
        <div>
          <h1 contenteditable="true" data-edit-field="name">${escapeHtml(resumeProfile.value.name || '你的姓名')}</h1>
          <p contenteditable="true" data-edit-field="targetRole">${escapeHtml(resumeProfile.value.targetRole || 'Java 后端开发实习生')}</p>
        </div>
        ${renderTopRightBlock('fresh')}
      </header>
      <div class="resume-body">${renderSections()}</div>
    </section>`
}

function renderTemplateClassic() {
  return `${baseResumeStyle(currentResumeAccent.value, resumeFontFamily.value)}
    <section class="resume-template resume-template--classic">
      <header class="resume-classic-head">
        <h1 contenteditable="true" data-edit-field="name">${escapeHtml(resumeProfile.value.name || '你的姓名')}</h1>
        <p contenteditable="true" data-edit-field="targetRole">${escapeHtml(resumeProfile.value.targetRole || 'Java 后端开发实习生')}</p>
        ${renderTopRightBlock('classic')}
      </header>
      <div class="resume-body">${renderSections()}</div>
    </section>`
}

function renderTemplateNavyBusiness() {
  return `${baseResumeStyle(currentResumeAccent.value, resumeFontFamily.value)}
    <section class="resume-template resume-template--navy">
      <header class="resume-navy-head">
        <div class="resume-navy-title">
          <span>Career Resume</span>
          <h1 ${editableFieldAttrs('name')}>${escapeHtml(resumeProfile.value.name || '你的姓名')}</h1>
          <p ${editableFieldAttrs('targetRole')}>${escapeHtml(resumeProfile.value.targetRole || 'Java 后端开发实习生')}</p>
        </div>
        ${renderTopRightBlock('navy')}
      </header>
      <div class="resume-navy-body">${renderSections()}</div>
    </section>`
}

function renderTemplateOrangeModern() {
  return `${baseResumeStyle(currentResumeAccent.value, resumeFontFamily.value)}
    <section class="resume-template resume-template--orange">
      <div class="resume-orange-rail"></div>
      <header class="resume-orange-head">
        <div>
          <h1 ${editableFieldAttrs('name')}>${escapeHtml(resumeProfile.value.name || '你的姓名')}</h1>
          <p ${editableFieldAttrs('targetRole')}>${escapeHtml(resumeProfile.value.targetRole || 'Java 后端开发实习生')}</p>
        </div>
        ${renderTopRightBlock('orange')}
      </header>
      <div class="resume-orange-body">${renderSections()}</div>
    </section>`
}

function renderTemplateSlateCard() {
  return `${baseResumeStyle(currentResumeAccent.value, resumeFontFamily.value)}
    <section class="resume-template resume-template--card">
      <header class="resume-card-head">
        <div>
          <h1 ${editableFieldAttrs('name')}>${escapeHtml(resumeProfile.value.name || '你的姓名')}</h1>
          <p ${editableFieldAttrs('targetRole')}>${escapeHtml(resumeProfile.value.targetRole || 'Java 后端开发实习生')}</p>
        </div>
        ${renderTopRightBlock('card')}
      </header>
      <div class="resume-card-body">${renderSections()}</div>
    </section>`
}

function renderTemplateRedLine() {
  return `${baseResumeStyle(currentResumeAccent.value, resumeFontFamily.value)}
    <section class="resume-template resume-template--redline">
      <header class="resume-redline-head">
        <div>
          <h1 ${editableFieldAttrs('name')}>${escapeHtml(resumeProfile.value.name || '你的姓名')}</h1>
          <p ${editableFieldAttrs('targetRole')}>${escapeHtml(resumeProfile.value.targetRole || 'Java 后端开发实习生')}</p>
        </div>
        ${renderTopRightBlock('redline')}
      </header>
      <div class="resume-redline-body">${renderSections()}</div>
    </section>`
}

function baseResumeStyle(accent, fontFamily) {
  return `<style>
    .resume-template {
      width: 794px;
      min-height: 1123px;
      margin: 0 auto;
      overflow: hidden;
      background: #ffffff;
      color: #172033;
      font-family: ${fontFamily};
      line-height: 1.62;
      box-shadow: 0 18px 46px rgba(15, 23, 42, 0.12);
    }
    .resume-template * { box-sizing: border-box; }
    .resume-template,
    .resume-template * {
      -webkit-print-color-adjust: exact;
      print-color-adjust: exact;
    }
    .resume-template [contenteditable="true"] {
      outline: 0;
      border-radius: 6px;
      transition: box-shadow .15s ease, background .15s ease, transform .15s ease;
      cursor: text;
    }
    .resume-template [contenteditable="true"]:hover {
      box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.28), 0 8px 22px rgba(15, 23, 42, 0.08);
      background: rgba(255, 255, 255, 0.32);
    }
    .resume-template [contenteditable="true"]:focus {
      box-shadow: 0 0 0 2px #409eff;
      background: rgba(255, 255, 255, 0.46);
    }
    .resume-hero {
      display: grid;
      grid-template-columns: minmax(0, 1fr) auto;
      align-items: start;
      gap: 24px;
      padding: 26px 34px 22px;
      background-color: ${accent};
      min-height: 180px;
      background: linear-gradient(135deg, ${accent}, #172033);
      color: #ffffff;
    }
    .resume-template--tech .resume-hero__left {
      display: grid;
      gap: 12px;
      align-content: start;
      min-width: 0;
    }
    .resume-template--tech .resume-hero__title {
      display: grid;
      gap: 8px;
      min-width: 0;
    }
    .resume-template--tech .resume-hero h1 {
      margin: 0;
      font-size: 34px;
      line-height: 1.05;
      letter-spacing: 1px;
    }
    .resume-template--tech .resume-hero p {
      margin: 0;
      font-size: 17px;
      font-weight: 700;
      opacity: 0.96;
    }
    .resume-template--tech .resume-hero__meta {
      display: flex;
      flex-wrap: wrap;
      gap: 8px 14px;
      max-width: 100%;
    }
    .resume-template--tech .resume-hero__meta .resume-contact {
      display: flex;
      flex-wrap: wrap;
      gap: 8px 14px;
    }
    .resume-template--tech .resume-hero__meta .resume-contact span {
      display: inline-flex;
      align-items: center;
      min-height: 28px;
      padding: 4px 10px;
      border-radius: 999px;
      background: rgba(255, 255, 255, 0.16);
      color: rgba(255, 255, 255, 0.96);
      backdrop-filter: blur(4px);
    }
    .resume-template--tech .resume-hero__photo {
      display: grid;
      justify-items: end;
      align-content: start;
      padding-top: 2px;
    }
    .resume-template--tech .resume-avatar--hero {
      width: 104px;
      height: 132px;
    }
    .resume-template--tech .resume-avatar--hero.resume-avatar--placeholder {
      padding: 10px;
    }
    .resume-template h1 { margin: 0; font-size: 30px; line-height: 1.2; letter-spacing: 1px; }
    .resume-template h2 { margin: 0 0 10px; color: ${accent}; font-size: 16px; letter-spacing: 1px; }
    .resume-template p { margin: 0; }
    .resume-template ul { margin: 0; padding-left: 18px; }
    .resume-template li { margin: 4px 0; }
    .resume-body { padding: 22px 42px 36px; }
    .resume-template--tech .resume-body { padding: 22px 34px 34px; }
    .resume-template--tech .resume-section { margin-bottom: 16px; }
    .resume-template:not(.resume-template--card) .resume-section { margin-bottom: 15px; }
    .resume-section-title {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 8px;
      font-weight: 900;
    }
    .resume-section-title[contenteditable="true"] {
      padding: 2px 6px;
      margin-left: -6px;
      min-height: 22px;
    }
    .resume-section-title::before {
      content: "";
      width: 5px;
      height: 18px;
      border-radius: 999px;
      background: ${accent};
    }
    .resume-section-content {
      color: #344258;
      font-size: 13px;
      line-height: 1.65;
      min-height: 28px;
      padding: 4px 6px;
      margin: -4px -6px 0;
      white-space: pre-line;
    }
    .resume-contact {
      display: grid;
      gap: 6px;
      font-size: 12px;
      opacity: 0.92;
    }
    .resume-top-right {
      display: grid;
      justify-items: end;
      align-content: start;
      gap: 10px;
      min-width: 180px;
    }
    .resume-avatar {
      width: 94px;
      height: 118px;
      overflow: hidden;
      border: 2px solid rgba(255, 255, 255, 0.82);
      border-radius: 18px;
      background: rgba(255, 255, 255, 0.12);
      box-shadow: 0 10px 26px rgba(15, 23, 42, 0.14);
    }
    .resume-avatar img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }
    .resume-avatar--placeholder {
      display: grid;
      place-items: center;
      gap: 3px;
      padding: 12px;
      color: #ffffff;
      text-align: center;
    }
    .resume-avatar--placeholder span {
      display: block;
      font-size: 22px;
      font-weight: 900;
      line-height: 1.1;
    }
    .resume-avatar--placeholder small {
      font-size: 11px;
      opacity: 0.88;
    }
    .resume-avatar--hero,
    .resume-avatar--fresh {
      width: 92px;
      height: 116px;
    }
    .resume-avatar--portrait {
      width: 94px;
      height: 118px;
      border-radius: 18px;
    }
    .resume-avatar--square {
      width: 118px;
      height: 118px;
      border-radius: 18px;
    }
    .resume-avatar--side {
      width: 116px;
      height: 144px;
      border-radius: 20px;
    }
    .resume-avatar--classic,
    .resume-avatar--card,
    .resume-avatar--redline {
      width: 88px;
      height: 110px;
    }
    .resume-avatar--navy {
      width: 100px;
      height: 124px;
      border-radius: 16px;
    }
    .resume-avatar--orange {
      width: 92px;
      height: 118px;
      border-radius: 22px;
    }
    .resume-avatar--fresh,
    .resume-avatar--orange,
    .resume-avatar--card,
    .resume-avatar--classic {
      border-color: rgba(15, 23, 42, 0.18);
      background: rgba(255, 255, 255, 0.92);
      color: #172033;
    }
    .resume-avatar--fresh.resume-avatar--placeholder,
    .resume-avatar--orange.resume-avatar--placeholder,
    .resume-avatar--card.resume-avatar--placeholder,
    .resume-avatar--classic.resume-avatar--placeholder {
      color: #172033;
    }
    .resume-top-right--fresh .resume-contact,
    .resume-top-right--card .resume-contact,
    .resume-top-right--classic .resume-contact {
      color: #475569;
    }
    .resume-top-right--side {
      justify-items: center;
      gap: 12px;
      width: 100%;
      min-width: 0;
    }
    .resume-top-right--side .resume-avatar {
      margin-bottom: 2px;
    }
    .resume-top-right--side .resume-contact {
      justify-items: center;
      text-align: center;
    }
    .resume-template--purple {
      display: grid;
      grid-template-columns: 218px 1fr;
    }
    .resume-template--purple .resume-side {
      padding: 28px 22px;
      background-color: ${accent};
      background: linear-gradient(180deg, ${accent}, #3b0764);
      color: #ffffff;
      display: grid;
      align-content: start;
      gap: 12px;
    }
    .resume-template--purple .resume-side h1 { font-size: 28px; line-height: 1.08; }
    .resume-template--purple .resume-side p { margin: 4px 0 12px; font-weight: 800; opacity: 0.94; }
    .resume-template--purple .resume-main { padding: 28px 34px 36px; }
    .resume-template--fresh { border-top: 10px solid ${accent}; }
    .resume-fresh-head {
      display: flex;
      justify-content: space-between;
      gap: 20px;
      margin: 22px 42px 0;
      padding: 18px 20px;
      border-radius: 20px;
      background-color: #ecfdf5;
      background: linear-gradient(135deg, #ecfdf5, #ffffff);
      border: 1px solid rgba(16, 185, 129, 0.18);
    }
    .resume-template--classic { box-shadow: 0 18px 46px rgba(15, 23, 42, 0.08); }
    .resume-classic-head {
      display: grid;
      grid-template-columns: minmax(0, 1fr) 220px;
      gap: 20px;
      margin: 26px 42px 0;
      padding-bottom: 14px;
      border-bottom: 2px solid #111827;
      text-align: left;
    }
    .resume-classic-head .resume-contact {
      margin-top: 4px;
      color: #475569;
    }
    .resume-template--navy {
      background: linear-gradient(180deg, #f8fafc 0%, #ffffff 36%);
    }
    .resume-navy-head {
      display: grid;
      grid-template-columns: minmax(0, 1fr) 220px;
      gap: 28px;
      padding: 30px 44px 24px;
      border-bottom: 1px solid #dbeafe;
      background-color: #0f172a;
      background: linear-gradient(135deg, #0f172a, ${accent});
      color: #ffffff;
    }
    .resume-navy-title > span {
      display: inline-block;
      margin-bottom: 12px;
      color: #93c5fd;
      font-size: 12px;
      font-weight: 800;
      letter-spacing: 2px;
      text-transform: uppercase;
    }
    .resume-navy-title p { margin-top: 6px; color: #bfdbfe; font-weight: 700; }
    .resume-top-right--navy { align-self: end; padding: 14px; border-radius: 14px; background: rgba(255, 255, 255, 0.1); }
    .resume-top-right--navy .resume-avatar {
      border-color: rgba(191, 219, 254, 0.9);
      box-shadow: 0 12px 26px rgba(15, 23, 42, 0.22);
    }
    .resume-top-right--navy .resume-contact {
      color: #e0f2fe;
    }
    .resume-navy-body { padding: 24px 44px 40px; }
    .resume-template--navy .resume-section-title::before { width: 22px; height: 3px; border-radius: 0; }
    .resume-template--orange {
      position: relative;
      padding-left: 22px;
    }
    .resume-orange-rail {
      position: absolute;
      inset: 0 auto 0 0;
      width: 22px;
      background: linear-gradient(180deg, ${accent}, #fed7aa);
    }
    .resume-orange-head {
      display: flex;
      justify-content: space-between;
      gap: 20px;
      padding: 28px 42px 20px;
      background-color: #fff7ed;
      background: linear-gradient(135deg, #fff7ed, #ffffff);
      border-bottom: 1px solid #fed7aa;
    }
    .resume-orange-head h1 { color: #9a3412; }
    .resume-orange-head p { color: #c2410c; font-weight: 800; }
    .resume-top-right--orange .resume-contact { color: #7c2d12; }
    .resume-top-right--orange .resume-avatar {
      border-color: rgba(249, 115, 22, 0.28);
      box-shadow: 0 14px 28px rgba(249, 115, 22, 0.18);
    }
    .resume-orange-body { padding: 24px 42px 38px; }
    .resume-template--orange .resume-section {
      padding: 11px 0 13px;
      border-bottom: 1px dashed #fed7aa;
    }
    .resume-template--card {
      padding: 24px 34px 34px;
      background-color: #f1f5f9;
      background: linear-gradient(180deg, #f8fafc, #eef2f7);
    }
    .resume-card-head {
      display: flex;
      justify-content: space-between;
      gap: 24px;
      padding: 20px 24px;
      border-radius: 22px;
      background: #ffffff;
      box-shadow: 0 16px 34px rgba(15, 23, 42, 0.09);
    }
    .resume-card-head h1 { color: #0f172a; }
    .resume-card-head p { margin-top: 7px; color: ${accent}; font-weight: 800; }
    .resume-top-right--card .resume-avatar { border-color: rgba(71, 85, 105, 0.28); }
    .resume-top-right--card .resume-contact {
      color: #64748b;
    }
    .resume-card-body {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 14px;
      margin-top: 14px;
    }
    .resume-template--card .resume-section {
      margin: 0;
      padding: 15px 16px;
      border-radius: 18px;
      background: #ffffff;
      box-shadow: 0 12px 28px rgba(15, 23, 42, 0.06);
    }
    .resume-template--card .resume-section:nth-child(1),
    .resume-template--card .resume-section:nth-child(3) {
      grid-column: 1 / -1;
    }
    .resume-template--redline {
      border-top: 12px solid ${accent};
      background: linear-gradient(90deg, #ffffff 0%, #ffffff 72%, #f8fafc 72%, #f8fafc 100%);
    }
    .resume-redline-head {
      display: grid;
      grid-template-columns: minmax(0, 1fr) 230px;
      gap: 28px;
      margin: 26px 42px 0;
      padding-bottom: 14px;
      border-bottom: 3px solid #111827;
    }
    .resume-redline-head h1 { color: #111827; }
    .resume-redline-head p { margin-top: 8px; color: ${accent}; font-weight: 900; }
    .resume-top-right--redline .resume-contact {
      padding-left: 18px;
      border-left: 3px solid ${accent};
      color: #334155;
    }
    .resume-top-right--redline .resume-avatar {
      border-color: rgba(220, 38, 38, 0.28);
    }
    .resume-redline-body { padding: 24px 42px 38px; }
    .resume-template--redline .resume-section-title {
      padding-bottom: 6px;
      border-bottom: 1px solid #e2e8f0;
    }
    .resume-template--redline .resume-section-title::before {
      width: 12px;
      height: 12px;
      border-radius: 50%;
    }
    @media print {
      .resume-template {
        width: 210mm;
        min-height: 297mm;
        box-shadow: none;
        background-color: #ffffff !important;
      }
      .resume-template,
      .resume-template * {
        -webkit-print-color-adjust: exact !important;
        print-color-adjust: exact !important;
      }
      .resume-hero {
        background-color: ${accent} !important;
        background: linear-gradient(135deg, ${accent}, #172033) !important;
      }
      .resume-template--purple .resume-side {
        background-color: ${accent} !important;
        background: linear-gradient(180deg, ${accent}, #3b0764) !important;
      }
      .resume-fresh-head {
        background-color: #ecfdf5 !important;
        background: linear-gradient(135deg, #ecfdf5, #ffffff) !important;
        border-color: rgba(16, 185, 129, 0.18) !important;
      }
      .resume-template--navy {
        background: linear-gradient(180deg, #f8fafc 0%, #ffffff 36%) !important;
      }
      .resume-navy-head {
        background-color: #0f172a !important;
        background: linear-gradient(135deg, #0f172a, ${accent}) !important;
      }
      .resume-template--orange {
        background-color: #ffffff !important;
      }
      .resume-orange-rail {
        background: linear-gradient(180deg, ${accent}, #fed7aa) !important;
      }
      .resume-orange-head {
        background-color: #fff7ed !important;
        background: linear-gradient(135deg, #fff7ed, #ffffff) !important;
        border-color: #fed7aa !important;
      }
      .resume-template--card {
        background-color: #f1f5f9 !important;
        background: linear-gradient(180deg, #f8fafc, #eef2f7) !important;
      }
      .resume-card-head,
      .resume-template--card .resume-section {
        background-color: #ffffff !important;
      }
      .resume-template--redline {
        background: linear-gradient(90deg, #ffffff 0%, #ffffff 72%, #f8fafc 72%, #f8fafc 100%) !important;
        border-top-color: ${accent} !important;
      }
      .resume-section-title::before {
        background-color: ${accent} !important;
      }
      .resume-avatar,
      .resume-top-right--navy {
        -webkit-print-color-adjust: exact !important;
        print-color-adjust: exact !important;
      }
    }
  </style>`
}

function renderContactItems() {
  return [
    ['phone', '电话', resumeProfile.value.phone],
    ['email', '邮箱', resumeProfile.value.email],
    ['location', '所在地', resumeProfile.value.location]
  ]
    .filter(([, , value]) => value)
    .map(([field, label, value]) => `<span ${editableFieldAttrs(field)}>${label}：${escapeHtml(value)}</span>`)
    .join('')
}

function renderSections() {
  return visibleResumeSections.value.map((section) => `
    <section class="resume-section">
      <h2 class="resume-section-title" ${editableSectionTitleAttrs(section.key)}>${escapeHtml(section.label)}</h2>
      <div class="resume-section-content" ${editableSectionAttrs(section.key)}>${formatSectionContent(sectionContent(section.key))}</div>
    </section>
  `).join('')
}

function sectionContent(key) {
  return resumeProfile.value[key] || ''
}

function formatSectionContent(value) {
  const lines = value.split('\n').map((line) => line.trim()).filter(Boolean)
  if (lines.length > 1) {
    return `<ul>${lines.map((line) => `<li>${inlineMarkdown(line.replace(/^[-*]\s*/, ''))}</li>`).join('')}</ul>`
  }
  return inlineMarkdown(value)
}

function buildResumeMarkdownFromProfile() {
  const contact = [
    resumeProfile.value.phone,
    resumeProfile.value.email,
    resumeProfile.value.location
  ].filter(Boolean).join(' ｜ ')
  const sections = visibleResumeSections.value
    .map((section) => `## ${section.label}\n\n${sectionContent(section.key)}`)
    .join('\n\n')
  return `# ${resumeProfile.value.name || '我的简历'}\n\n**求职意向：** ${resumeProfile.value.targetRole || ''}\n\n${contact ? `**联系方式：** ${contact}\n\n` : ''}${sections}`.trim()
}

function markdownToHtml(markdown) {
  const lines = markdown.split(/\r?\n/)
  const html = []
  let listType = null

  const closeList = () => {
    if (listType) {
      html.push(`</${listType}>`)
      listType = null
    }
  }

  lines.forEach((line) => {
    const trimmed = line.trim()
    if (!trimmed) {
      closeList()
      return
    }

    if (trimmed.startsWith('### ')) {
      closeList()
      html.push(`<h3>${inlineMarkdown(trimmed.slice(4))}</h3>`)
      return
    }
    if (trimmed.startsWith('## ')) {
      closeList()
      html.push(`<h2>${inlineMarkdown(trimmed.slice(3))}</h2>`)
      return
    }
    if (trimmed.startsWith('# ')) {
      closeList()
      html.push(`<h1>${inlineMarkdown(trimmed.slice(2))}</h1>`)
      return
    }

    const orderedMatch = trimmed.match(/^\d+\.\s+(.+)$/)
    if (orderedMatch) {
      if (listType !== 'ol') {
        closeList()
        listType = 'ol'
        html.push('<ol>')
      }
      html.push(`<li>${inlineMarkdown(orderedMatch[1])}</li>`)
      return
    }

    const unorderedMatch = trimmed.match(/^[-*]\s+(.+)$/)
    if (unorderedMatch) {
      if (listType !== 'ul') {
        closeList()
        listType = 'ul'
        html.push('<ul>')
      }
      html.push(`<li>${inlineMarkdown(unorderedMatch[1])}</li>`)
      return
    }

    closeList()
    html.push(`<p>${inlineMarkdown(trimmed)}</p>`)
  })

  closeList()
  return html.join('\n')
}

function inlineMarkdown(value) {
  return escapeHtml(value)
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/`(.+?)`/g, '<code>$1</code>')
}

function wordSectionContent(value) {
  const lines = String(value || '')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)

  if (!lines.length) {
    return '<p style="color:#94a3b8;">可继续补充这一部分内容</p>'
  }

  if (lines.length === 1) {
    return `<p>${inlineMarkdown(lines[0])}</p>`
  }

  return `<ul>${lines.map((line) => `<li>${inlineMarkdown(line.replace(/^[-*]\s*/, ''))}</li>`).join('')}</ul>`
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
}

onMounted(() => {
  refreshAiStatus()
  refreshCurrentUser()
  refreshRecords()
})
</script>

<style scoped>
.app-shell {
  height: 100vh;
  padding: 14px 18px 18px;
  overflow: hidden;
  background: #eef2f8;
}

.app-header {
  height: 58px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 14px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.brand-mark {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  background: #111827;
  color: #ffffff;
  font-size: 13px;
  font-weight: 800;
}

.brand h1 {
  margin: 0;
  color: #172033;
  font-size: 22px;
  line-height: 1.2;
  letter-spacing: 0;
}

.brand p {
  margin: 3px 0 0;
  color: #69758a;
  font-size: 13px;
}

.header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.studio-grid {
  height: calc(100vh - 90px);
  display: grid;
  grid-template-columns: minmax(320px, 0.86fr) minmax(520px, 1.18fr) minmax(320px, 0.86fr);
  gap: 14px;
}

.studio-panel {
  min-height: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  overflow: hidden;
  border: 1px solid #d9e1ed;
  border-radius: 14px;
  background: #ffffff;
}

.panel-header {
  padding: 16px 18px;
  border-bottom: 1px solid #e4eaf3;
  background: #ffffff;
}

.panel-header h2 {
  margin: 0;
  color: #172033;
  font-size: 17px;
  letter-spacing: 0;
}

.panel-header p {
  margin: 5px 0 0;
  color: #7b8798;
  font-size: 12px;
}

.panel-scroll,
.tab-scroll,
.chat-messages {
  min-height: 0;
  padding: 16px 18px;
  overflow: auto;
}

.panel-footer {
  display: flex;
  gap: 10px;
  padding: 14px 18px;
  border-top: 1px solid #e4eaf3;
  background: #fbfcfe;
}

.panel-footer .el-button {
  flex: 1;
}

.upload-box {
  margin-bottom: 18px;
}

.resume-builder-button {
  width: 100%;
  margin-bottom: 4px;
}

.upload-mark {
  width: 42px;
  height: 30px;
  display: grid;
  place-items: center;
  margin: 0 auto 10px;
  border: 1px solid #bdd2f2;
  border-radius: 8px;
  background: #edf5ff;
  color: #2c6fbb;
  font-size: 12px;
  font-weight: 800;
}

.upload-title {
  color: #172033;
  font-size: 15px;
  font-weight: 800;
}

.upload-subtitle {
  margin-top: 6px;
  color: #8792a3;
  font-size: 12px;
}

.field-label {
  display: block;
  margin: 16px 0 8px;
  color: #344258;
  font-size: 13px;
  font-weight: 800;
}

.score-card {
  display: grid;
  grid-template-columns: 106px 1fr;
  gap: 18px;
  padding: 16px;
  border: 1px solid #dce6f3;
  border-radius: 12px;
  background: #f8fbff;
}

.score-number {
  min-height: 104px;
  display: grid;
  place-items: center;
  align-content: center;
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid #e4ebf5;
}

.score-number strong {
  color: #172033;
  font-size: 34px;
  line-height: 1;
}

.score-number span {
  margin-top: 4px;
  color: #6b778c;
  font-size: 13px;
}

.score-copy {
  min-width: 0;
}

.score-line {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
  align-items: center;
  margin-bottom: 10px;
}

.score-copy p {
  max-height: 116px;
  margin: 0;
  padding-right: 8px;
  overflow: auto;
  color: #3d4d63;
  font-size: 14px;
  line-height: 1.8;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-top: 12px;
}

.stat-card {
  padding: 12px;
  border: 1px solid #dfe7f2;
  border-radius: 10px;
  background: #ffffff;
}

.stat-card strong {
  display: block;
  color: #172033;
  font-size: 24px;
  line-height: 1;
}

.stat-card span {
  display: block;
  margin-top: 8px;
  color: #6d7890;
  font-size: 12px;
}

.priority-card {
  margin-top: 12px;
  padding: 14px 16px;
  border: 1px solid #f0d7a8;
  border-radius: 12px;
  background: #fffaf1;
}

.block-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

.priority-card ol {
  margin: 0;
  padding-left: 18px;
  color: #4a3920;
  font-size: 13px;
  line-height: 1.8;
}

.clean-collapse {
  margin-top: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
}

.clean-collapse :deep(.el-collapse-item__header) {
  padding: 0 16px;
  color: #172033;
  font-weight: 800;
}

.clean-collapse :deep(.el-collapse-item__content) {
  padding: 0 16px 16px;
}

.right-tabs {
  min-height: 0;
  height: 100%;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  overflow: hidden;
}

.right-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 18px;
}

.right-tabs :deep(.el-tabs__content),
.right-tabs :deep(.el-tab-pane) {
  min-height: 0;
  height: 100%;
  overflow: hidden;
}

.assistant-pane {
  height: 100%;
  min-height: 0;
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto;
}

.tab-scroll {
  min-height: 0;
  height: auto;
}

.empty-card {
  display: grid;
  gap: 6px;
  padding: 18px;
  border: 1px dashed #ccd7e6;
  border-radius: 12px;
  background: #fbfdff;
  color: #7b8798;
}

.empty-card strong {
  color: #344258;
}

.interview-list {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.interview-list li {
  display: grid;
  grid-template-columns: 28px 1fr;
  gap: 10px;
  padding: 12px;
  border: 1px solid #dfe7f2;
  border-radius: 10px;
  background: #ffffff;
}

.interview-list span {
  width: 26px;
  height: 26px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  background: #eef5ff;
  color: #2f6fbb;
  font-size: 12px;
  font-weight: 800;
}

.interview-list p {
  margin: 0;
  color: #344258;
  font-size: 14px;
  line-height: 1.7;
}

.chat-layout {
  height: 100%;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  min-height: 0;
}

.chat-history-panel {
  padding: 12px 16px 10px;
  border-bottom: 1px solid #e4eaf3;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.chat-history-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chat-history-tip {
  margin-left: auto;
  color: #8492a6;
  font-size: 12px;
}

.chat-session-list {
  display: grid;
  gap: 8px;
  max-height: 132px;
  overflow-y: auto;
  margin-top: 10px;
  padding-right: 4px;
}

.chat-session-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 4px 8px;
  align-items: center;
  width: 100%;
  padding: 9px 10px;
  border: 1px solid #e2e9f4;
  border-radius: 12px;
  background: #ffffff;
  color: #26364d;
  text-align: left;
  cursor: pointer;
}

.chat-session-item span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 700;
}

.chat-session-item small {
  grid-column: 1;
  color: #93a0b3;
  font-size: 11px;
}

.chat-session-item--active {
  border-color: #2f7df6;
  background: #eef5ff;
}

.chat-session-delete {
  grid-column: 2;
  grid-row: 1 / span 2;
}

.chat-messages {
  display: grid;
  align-content: start;
  gap: 10px;
  overflow-y: auto;
  padding: 16px 18px;
}

.chat-bubble {
  max-width: 92%;
  padding: 12px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.75;
}

.chat-bubble p {
  margin: 0;
  white-space: pre-wrap;
}

.chat-bubble--user {
  justify-self: end;
  background: #2f7df6;
  color: #ffffff;
}

.chat-bubble--assistant {
  justify-self: start;
  border: 1px solid #dfe7f2;
  background: #ffffff;
  color: #344258;
}

.chat-input {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 74px;
  align-items: end;
  gap: 10px;
  padding: 14px 18px;
  border-top: 1px solid #e4eaf3;
  background: #fbfcfe;
}

.chat-input :deep(.el-textarea__inner) {
  min-height: 72px !important;
  max-height: 92px !important;
}

.send-button {
  height: 72px;
}

.settings-form {
  margin-top: 16px;
}

.auth-dialog :deep(.el-dialog) {
  border-radius: 18px;
  overflow: hidden;
}

.auth-dialog :deep(.el-dialog__header) {
  margin: 0;
  padding: 24px 26px 10px;
}

.auth-dialog :deep(.el-dialog__body) {
  padding: 10px 26px 8px;
}

.auth-dialog :deep(.el-dialog__footer) {
  padding: 10px 26px 24px;
}

.auth-header {
  display: flex;
  align-items: center;
  gap: 14px;
}

.auth-logo {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: linear-gradient(135deg, #111827 0%, #2563eb 100%);
  color: #ffffff;
  font-size: 14px;
  font-weight: 900;
  box-shadow: 0 10px 22px rgba(37, 99, 235, 0.22);
}

.auth-header h2 {
  margin: 0;
  color: #172033;
  font-size: 22px;
  line-height: 1.2;
}

.auth-header p {
  margin: 6px 0 0;
  color: #7a8699;
  font-size: 13px;
}

.auth-tip {
  border: 0;
  border-radius: 14px;
  background: #f5f7fb;
}

.auth-tip :deep(.el-alert__title) {
  color: #7c8798;
  font-size: 13px;
  line-height: 1.7;
}

.auth-form {
  display: grid;
  gap: 2px;
}

.auth-form :deep(.el-form-item__label) {
  color: #344258;
  font-size: 13px;
  font-weight: 800;
}

.auth-form :deep(.el-input__wrapper) {
  border-radius: 12px;
  box-shadow: 0 0 0 1px #d8e1ee inset;
}

.auth-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #409eff inset, 0 0 0 4px rgba(64, 158, 255, 0.1);
}

.code-row {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 126px auto;
  gap: 10px;
  align-items: center;
}

.code-row .el-tag {
  height: 38px;
  padding: 0 12px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 800;
  letter-spacing: 1px;
}

.auth-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.auth-link {
  color: #64748b;
  font-weight: 700;
}

.auth-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.auth-actions .el-button {
  min-width: 88px;
  border-radius: 12px;
  font-weight: 800;
}

.resume-builder-dialog :deep(.el-dialog__body) {
  padding-top: 8px;
}

.resume-studio {
  height: 74vh;
  min-height: 640px;
  display: grid;
  grid-template-columns: minmax(320px, 0.72fr) minmax(520px, 1.28fr);
  gap: 14px;
  overflow: hidden;
}

.resume-editor-panel,
.resume-section-panel,
.visual-resume-preview {
  min-height: 0;
  overflow: auto;
  border: 1px solid #dfe7f2;
  border-radius: 14px;
  background: #fbfdff;
}

.resume-editor-panel {
  padding: 14px;
}

.builder-feature-grid {
  display: grid;
  gap: 12px;
  margin-bottom: 14px;
}

.builder-feature-card {
  padding: 12px;
  border: 1px solid #e0e8f3;
  border-radius: 14px;
  background: #f8fbff;
}

.builder-feature-card__head {
  display: grid;
  gap: 4px;
  margin-bottom: 10px;
}

.builder-feature-card__head strong {
  color: #172033;
  font-size: 13px;
  font-weight: 900;
}

.builder-feature-card__head span {
  color: #7b8798;
  font-size: 11px;
}

.builder-mini-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
}

.builder-font-select {
  width: 100%;
}

.photo-preview-box {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  margin-bottom: 10px;
}

.photo-preview-frame {
  width: 96px;
  height: 124px;
  overflow: hidden;
  border: 1px dashed #c9d5e4;
  border-radius: 16px;
  background: #ffffff;
  cursor: pointer;
  transition: transform .15s ease, box-shadow .15s ease, border-color .15s ease;
}

.photo-preview-frame:hover {
  transform: translateY(-1px);
  border-color: #8cb7ff;
  box-shadow: 0 8px 18px rgba(47, 125, 246, 0.12);
}

.photo-preview-frame--square {
  border-radius: 18px;
}

.photo-preview-frame--portrait {
  border-radius: 16px;
}

.photo-preview-frame img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.photo-preview-placeholder {
  display: grid;
  place-items: center;
  height: 100%;
  color: #5b6b82;
  font-size: 22px;
  font-weight: 900;
}

.photo-preview-text p {
  margin: 0;
  color: #5b6b82;
  font-size: 12px;
  line-height: 1.6;
}

.photo-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.photo-input {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  border: 0;
}

.template-strip {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.template-card {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 3px 8px;
  align-items: center;
  padding: 10px;
  border: 1px solid #dfe7f2;
  border-radius: 12px;
  background: #ffffff;
  color: #344258;
  text-align: left;
  cursor: pointer;
}

.template-card span {
  width: 18px;
  height: 18px;
  grid-row: span 2;
  border-radius: 6px;
}

.template-card strong {
  color: #172033;
  font-size: 13px;
}

.template-card small {
  color: #7b8798;
  font-size: 11px;
}

.template-card--active {
  border-color: #409eff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.12);
}

.resume-builder-form {
  min-width: 0;
}

.builder-two-cols,
.builder-three-cols {
  display: grid;
  gap: 12px;
}

.builder-two-cols {
  grid-template-columns: 0.7fr 1.3fr;
}

.builder-three-cols {
  grid-template-columns: repeat(3, 1fr);
}

.resume-section-panel {
  padding: 14px;
}

.section-control-list {
  display: grid;
  gap: 10px;
}

.section-control {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto auto auto;
  gap: 8px;
  align-items: center;
  padding: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #ffffff;
}

.section-control .el-input {
  min-width: 0;
}

.section-control .el-button {
  margin-left: 0;
}

.source-collapse {
  margin-top: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
}

.visual-resume-preview {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  background: #eef2f8;
}

.visual-preview-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-bottom: 1px solid #dfe7f2;
  background: #ffffff;
}

.visual-preview-toolbar strong {
  display: block;
  color: #172033;
  font-size: 14px;
}

.visual-preview-toolbar span {
  color: #7b8798;
  font-size: 12px;
}

.resume-zoom-controls {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.resume-zoom-controls > span {
  min-width: 42px;
  color: #344258;
  font-size: 12px;
  font-weight: 800;
  text-align: center;
}

.resume-zoom-controls .el-button {
  margin-left: 0;
}

.a4-scroll {
  min-height: 0;
  overflow: auto;
  padding: 22px;
}

.a4-stage {
  position: relative;
  margin: 0;
}

.a4-page {
  position: absolute;
  top: 0;
  left: 0;
  transform-origin: top left;
}

.resume-builder-footer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.resume-builder-actions,
.resume-export-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.resume-export-actions {
  justify-content: flex-end;
}

.resume-builder-actions .el-button,
.resume-export-actions .el-button {
  margin-left: 0;
}

.record-list {
  display: grid;
  gap: 12px;
}

.record-card {
  padding: 14px;
  border: 1px solid #dfe7f2;
  border-radius: 12px;
  background: #ffffff;
}

.record-card__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.record-title {
  min-width: 0;
  padding: 0;
  overflow: hidden;
  border: 0;
  background: transparent;
  color: #172033;
  font: inherit;
  font-weight: 800;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

.record-title:hover {
  color: #2f7df6;
}

.record-card span {
  flex: 0 0 auto;
  color: #8a95a8;
  font-size: 12px;
}

.record-card p {
  margin: 10px 0 0;
  color: #526176;
  font-size: 13px;
  line-height: 1.6;
}

.record-card__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}

@media (max-width: 1180px) {
  .app-shell {
    height: auto;
    overflow: auto;
  }

  .studio-grid {
    height: auto;
    grid-template-columns: 1fr;
  }

  .studio-panel {
    min-height: 520px;
  }

  .resume-studio,
  .builder-two-cols,
  .builder-three-cols {
    grid-template-columns: 1fr;
  }

  .resume-studio {
    height: auto;
    overflow: visible;
  }

  .resume-builder-footer {
    grid-template-columns: 1fr;
  }

  .resume-export-actions {
    justify-content: flex-start;
  }
}
</style>
