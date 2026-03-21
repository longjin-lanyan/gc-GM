package com.genshin.gm.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.genshin.gm.data.local.SessionManager
import com.genshin.gm.data.proto.ProtoClient
import com.genshin.gm.data.repository.ResourceManager
import com.genshin.gm.proto.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UiState(
    val isLoggedIn: Boolean = false,
    val username: String = "",
    val sessionToken: String = "",
    val activeUid: String = "",
    val verifiedUids: List<String> = emptyList(),
    val serverUrl: String = "http://127.0.0.1:8080",
    val isLoading: Boolean = false,
    val message: String = "",
    val items: List<GameDataItem> = emptyList(),
    val weapons: List<GameDataItem> = emptyList(),
    val avatars: List<GameDataItem> = emptyList(),
    val quests: List<GameDataItem> = emptyList(),
    val approvedCommands: List<PlayerCommandProto> = emptyList(),
    val pendingCommands: List<PlayerCommandProto> = emptyList(),
    val resourceSyncStatus: String = "",
    val generatedCommand: String = "",
    val executeResult: String = "",
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val sessionManager = SessionManager(app)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var protoClient: ProtoClient? = null
    private var resourceManager: ResourceManager? = null

    init {
        viewModelScope.launch {
            val url = sessionManager.getServerUrl()
            val token = sessionManager.getSessionToken()
            val username = sessionManager.getUsername()
            val uid = sessionManager.getActiveUid()

            _state.update { it.copy(serverUrl = url) }
            initClient(url)

            // 每次启动自动热更资源
            syncResources()

            if (token != null && username != null) {
                _state.update {
                    it.copy(
                        isLoggedIn = true,
                        sessionToken = token,
                        username = username,
                        activeUid = uid ?: ""
                    )
                }
                refreshUserInfo()
            }
        }
    }

    private fun initClient(url: String) {
        protoClient = ProtoClient(url)
        resourceManager = ResourceManager(getApplication(), url)
    }

    fun updateServerUrl(url: String) {
        viewModelScope.launch {
            sessionManager.saveServerUrl(url)
            _state.update { it.copy(serverUrl = url) }
            initClient(url)
        }
    }

    // ==================== Auth ====================

    fun register(username: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, message = "") }
            try {
                val resp = protoClient!!.register(username, password)
                _state.update { it.copy(isLoading = false, message = resp.message) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, message = "注册失败: ${e.message}") }
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, message = "") }
            try {
                val resp = protoClient!!.login(username, password)
                if (resp.success) {
                    sessionManager.saveLogin(resp.sessionToken, resp.username)
                    _state.update {
                        it.copy(
                            isLoading = false, isLoggedIn = true,
                            sessionToken = resp.sessionToken,
                            username = resp.username,
                            message = "登录成功"
                        )
                    }
                    refreshUserInfo()
                } else {
                    _state.update { it.copy(isLoading = false, message = resp.message) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, message = "登录失败: ${e.message}") }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                protoClient?.logout(_state.value.sessionToken)
            } catch (_: Exception) {}
            sessionManager.clearLogin()
            _state.update {
                it.copy(
                    isLoggedIn = false, sessionToken = "", username = "",
                    activeUid = "", verifiedUids = emptyList(), message = "已登出"
                )
            }
        }
    }

    private fun refreshUserInfo() {
        viewModelScope.launch {
            try {
                val resp = protoClient!!.getUserInfo(_state.value.sessionToken)
                if (resp.success) {
                    _state.update { it.copy(verifiedUids = resp.verifiedUidsList) }
                }
            } catch (_: Exception) {}
        }
    }

    fun setActiveUid(uid: String) {
        viewModelScope.launch {
            sessionManager.setActiveUid(uid)
            _state.update { it.copy(activeUid = uid) }
        }
    }

    // ==================== Verification & UID Binding ====================

    fun sendVerificationCode(uid: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, message = "") }
            try {
                val resp = protoClient!!.sendVerificationCode(uid)
                _state.update { it.copy(isLoading = false, message = resp.message) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, message = "发送失败: ${e.message}") }
            }
        }
    }

    fun verifyCode(uid: String, code: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, message = "") }
            try {
                val resp = protoClient!!.verifyCode(uid, code)
                _state.update { it.copy(isLoading = false, message = resp.message) }
                if (resp.success) {
                    // Auto-bind UID after verification
                    bindUid(uid)
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, message = "验证失败: ${e.message}") }
            }
        }
    }

    fun bindUid(uid: String) {
        viewModelScope.launch {
            try {
                val resp = protoClient!!.addUid(_state.value.sessionToken, uid)
                _state.update { it.copy(message = resp.message) }
                if (resp.success) refreshUserInfo()
            } catch (e: Exception) {
                _state.update { it.copy(message = "绑定失败: ${e.message}") }
            }
        }
    }

    fun unbindUid(uid: String) {
        viewModelScope.launch {
            try {
                val resp = protoClient!!.removeUid(_state.value.sessionToken, uid)
                _state.update { it.copy(message = resp.message) }
                if (resp.success) {
                    refreshUserInfo()
                    if (_state.value.activeUid == uid) {
                        setActiveUid("")
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(message = "解绑失败: ${e.message}") }
            }
        }
    }

    // ==================== Game Data ====================

    fun loadGameData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                // 优先从本地热更缓存加载
                val rm = resourceManager
                val localItems = rm?.parseGameData("Item.txt")
                val localWeapons = rm?.parseGameData("Weapon.txt")
                val localAvatars = rm?.parseGameData("Avatar.txt")
                val localQuests = rm?.parseGameData("Quest.txt")

                if (!localItems.isNullOrEmpty()) {
                    // 使用本地缓存数据
                    _state.update {
                        it.copy(
                            isLoading = false,
                            items = localItems.map { (id, name) ->
                                GameDataItem.newBuilder().setId(id).setName(name).build()
                            },
                            weapons = (localWeapons ?: emptyList()).map { (id, name) ->
                                GameDataItem.newBuilder().setId(id).setName(name).build()
                            },
                            avatars = (localAvatars ?: emptyList()).map { (id, name) ->
                                GameDataItem.newBuilder().setId(id).setName(name).build()
                            },
                            quests = (localQuests ?: emptyList()).map { (id, name) ->
                                GameDataItem.newBuilder().setId(id).setName(name).build()
                            },
                        )
                    }
                } else {
                    // 本地无缓存，从服务端 proto 接口拉取
                    val items = protoClient!!.getItems()
                    val weapons = protoClient!!.getWeapons()
                    val avatars = protoClient!!.getAvatars()
                    val quests = protoClient!!.getQuests()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            items = items.itemsList,
                            weapons = weapons.itemsList,
                            avatars = avatars.itemsList,
                            quests = quests.itemsList,
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, message = "加载数据失败: ${e.message}") }
            }
        }
    }

    fun generateGiveCommand(itemId: Int, quantity: Int) {
        viewModelScope.launch {
            try {
                val resp = protoClient!!.generateGiveCommand(itemId, quantity)
                _state.update { it.copy(generatedCommand = resp.command) }
            } catch (e: Exception) {
                _state.update { it.copy(message = "生成指令失败: ${e.message}") }
            }
        }
    }

    fun generateQuestCommand(questId: Int, isFinish: Boolean) {
        viewModelScope.launch {
            try {
                val resp = if (isFinish) protoClient!!.generateQuestFinishCommand(questId)
                else protoClient!!.generateQuestAddCommand(questId)
                _state.update { it.copy(generatedCommand = resp.command) }
            } catch (e: Exception) {
                _state.update { it.copy(message = "生成指令失败: ${e.message}") }
            }
        }
    }

    // ==================== Command Execution ====================

    fun executeCustomCommand(command: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, executeResult = "") }
            try {
                val resp = protoClient!!.executeCustomCommand(
                    _state.value.activeUid, command, _state.value.sessionToken
                )
                _state.update {
                    it.copy(
                        isLoading = false,
                        executeResult = if (resp.success) "成功: ${resp.data}" else "失败: ${resp.message}",
                        message = resp.message
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, executeResult = "执行失败: ${e.message}") }
            }
        }
    }

    fun executePresetCommand(id: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, executeResult = "") }
            try {
                val resp = protoClient!!.executePresetCommand(
                    id, _state.value.activeUid, _state.value.sessionToken
                )
                _state.update {
                    it.copy(
                        isLoading = false,
                        executeResult = if (resp.success) "成功: ${resp.data}" else "失败: ${resp.message}",
                        message = resp.message
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, executeResult = "执行失败: ${e.message}") }
            }
        }
    }

    // ==================== Player Commands ====================

    fun loadApprovedCommands(category: String = "", sort: String = "time") {
        viewModelScope.launch {
            try {
                val resp = protoClient!!.getApprovedCommands(category = category, sort = sort)
                _state.update { it.copy(approvedCommands = resp.commandsList) }
            } catch (e: Exception) {
                _state.update { it.copy(message = "加载指令失败: ${e.message}") }
            }
        }
    }

    fun submitCommand(title: String, description: String, command: String,
                      category: String, uploaderName: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, message = "") }
            try {
                val resp = protoClient!!.submitCommand(title, description, command, category, uploaderName)
                _state.update { it.copy(isLoading = false, message = resp.message) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, message = "提交失败: ${e.message}") }
            }
        }
    }

    fun likeCommand(id: Long) {
        viewModelScope.launch {
            try {
                val resp = protoClient!!.likeCommand(id, _state.value.activeUid)
                _state.update { it.copy(message = resp.message) }
                loadApprovedCommands()
            } catch (e: Exception) {
                _state.update { it.copy(message = "点赞失败: ${e.message}") }
            }
        }
    }

    // ==================== Admin ====================

    fun loadPendingCommands(adminToken: String) {
        viewModelScope.launch {
            try {
                val resp = protoClient!!.adminGetPending(adminToken)
                _state.update { it.copy(pendingCommands = resp.commandsList) }
            } catch (e: Exception) {
                _state.update { it.copy(message = "加载失败: ${e.message}") }
            }
        }
    }

    fun approveCommand(id: Long, adminToken: String, reviewNote: String = "") {
        viewModelScope.launch {
            try {
                protoClient!!.adminApprove(id, adminToken, reviewNote)
                _state.update { it.copy(message = "审核通过") }
                loadPendingCommands(adminToken)
            } catch (e: Exception) {
                _state.update { it.copy(message = "操作失败: ${e.message}") }
            }
        }
    }

    fun rejectCommand(id: Long, adminToken: String, reviewNote: String = "") {
        viewModelScope.launch {
            try {
                protoClient!!.adminReject(id, adminToken, reviewNote)
                _state.update { it.copy(message = "已拒绝") }
                loadPendingCommands(adminToken)
            } catch (e: Exception) {
                _state.update { it.copy(message = "操作失败: ${e.message}") }
            }
        }
    }

    // ==================== Resources ====================

    fun syncResources() {
        viewModelScope.launch {
            _state.update { it.copy(resourceSyncStatus = "正在同步资源...") }
            try {
                val updated = resourceManager!!.syncResources()
                _state.update {
                    it.copy(
                        resourceSyncStatus = if (updated.isEmpty()) "资源已是最新"
                        else "已更新 ${updated.size} 个文件"
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(resourceSyncStatus = "同步失败: ${e.message}") }
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = "") }
    }
}
