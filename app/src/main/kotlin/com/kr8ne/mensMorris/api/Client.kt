package com.kr8ne.mensMorris.api

import androidx.core.content.edit
import androidx.lifecycle.MutableLiveData
import com.kr8ne.mensMorris.activity
import com.kr8ne.mensMorris.move.Movement
import com.kr8ne.mensMorris.viewModel.impl.GameBoardViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.utils.io.printStack
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.LinkedList
import java.util.Queue

/**
 * Object representing the client for interacting with the server.
 */
object Client {
    private var userData: UserData = UserData("", "")

    /**
     * Jwt token provided by the server
     */
    @Volatile
    var jwtToken: String? = activity?.sharedPreferences?.getString("jwtToken", null)
        set(value) {
            field = value
            activity?.sharedPreferences?.edit(commit = true) {
                putString("jwtToken", value).apply()
            }
        }

    /**
     * current game id
     */
    @Volatile
    var gameId: Long? = activity?.sharedPreferences?.getString("gameId", null)?.toLongOrNull()
        set(value) {
            field = value
            activity?.sharedPreferences?.edit(commit = true) {
                putString("gameId", value.toString()).apply()
            }
        }

    /**
     * The server's address.
     * put your network ip here
     */
    const val SERVER_ADDRESS = "://10.68.154.156:8080"

    /**
     * The API endpoint for user-related operations.
     */
    const val USER_API = "/api/v1/user"

    /**
     * The network scope for asynchronous operations.
     */
    val networkScope = Dispatchers.IO

    /**
     * job created when searching for a game
     */
    var searchingForGameJob: Deferred<Result<Long>>? = null

    /**
     * queue of the moves that player performed
     * TODO: implement premoves with this one
     */
    val movesQueue: Queue<Movement> = LinkedList()

    /**
     * The network client for making HTTP requests.
     */
    val network = HttpClient(OkHttp) {
        install(WebSockets)
    }

    /**
     * Validates the provided login.
     *
     * @param login The login to be validated.
     * @return True if the login is valid, false otherwise.
     */
    fun loginValidator(login: String): Boolean {
        return login.length >= 6
    }

    /**
     * Validates the provided password.
     *
     * @param password The password to be validated.
     * @return True if the password is valid, false otherwise.
     */
    fun passwordValidator(password: String): Boolean {
        return password.length >= 6
    }

    /**
     * Updates the user's login.
     *
     * @param newLogin The new value of login.
     */
    fun updateUserLogin(newLogin: String) {
        userData.login = newLogin
    }

    /**
     * Updates the user's password.
     *
     * @param newLogin The new value of password.
     */
    fun updateUserPassword(newLogin: String) {
        userData.password = newLogin
    }


    /**
     * Attempts to register the user with the provided credentials.
     *
     * @return [ServerResponse] indicating the success or failure of the registration attempt.
     */
    suspend fun register(): Result<String> {
        val userDataState: UserData = userData
        return runCatching {
            val registerResult = network.get("http$SERVER_ADDRESS${USER_API}/reg") {
                method = HttpMethod.Get
                url {
                    parameters["login"] = userDataState.login
                    parameters["password"] = userDataState.password
                }
            }
            return when (registerResult.status.value) {
                200 -> {
                    Result.success(registerResult.bodyAsText())
                }

                401 -> {
                    Result.failure(ServerResponse.WrongPasswordOrLogin())
                }

                404 -> {
                    Result.failure(ServerResponse.Unreachable())
                }

                409 -> {
                    Result.failure(ServerResponse.LoginInUse())
                }

                else -> {
                    Result.failure(ServerResponse.UnknownServerError())
                }
            }
        }.onFailure {
            println("error accessing ${"http$SERVER_ADDRESS$USER_API/reg"}")
            it.printStack()
        }
    }

    /**
     * Attempts to login the user with the provided credentials.
     *
     * @return [ServerResponse] indicating the success or failure of the login attempt.
     */
    suspend fun login(): Result<String> {
        val userDataState: UserData = userData
        return runCatching {
            val loginResult = network.get("http$SERVER_ADDRESS${USER_API}/login") {
                method = HttpMethod.Get
                url {
                    parameters["login"] = userDataState.login
                    parameters["password"] = userDataState.password
                }
            }
            return when (loginResult.status.value) {
                200 -> {
                    Result.success(loginResult.bodyAsText())
                }

                401 -> {
                    Result.failure(ServerResponse.WrongPasswordOrLogin())
                }

                404 -> {
                    Result.failure(ServerResponse.Unreachable())
                }

                else -> {
                    Result.failure(ServerResponse.UnknownServerError())
                }
            }
        }.onFailure {
            println("error accessing ${"http$SERVER_ADDRESS$USER_API/login"}")
            it.printStack()
        }
    }

    /**
     * checks if we are currently playing a game
     */
    suspend fun isPlaying(): Long? {
        val jwtTokenState = jwtToken
        require(jwtTokenState != null)
        val result = network.get("http$SERVER_ADDRESS$USER_API/is-playing") {
            method = HttpMethod.Get
            url {
                parameters["jwtToken"] = jwtTokenState
            }
        }
        return result.bodyAsText().toLongOrNull()
    }

    /**
     * Starts searching for a game.
     *
     * @return [ServerResponse] indicating the success or failure of the search attempt.
     */
    fun startSearchingGame() {
        if (searchingForGameJob?.isCompleted == false) {
            return
        }
        searchingForGameJob = CoroutineScope(networkScope).async {
            runCatching {
                val jwtTokenState = jwtToken
                require(jwtTokenState != null)
                var gameId: String? = null
                network.webSocket("ws$SERVER_ADDRESS$USER_API/search-for-game", request = {
                    url {
                        parameters["jwtToken"] = jwtTokenState
                    }
                }) {
                    while (true) {
                        val serverMessage = (incoming.receive() as? Frame.Text)?.readText()
                        if (serverMessage != null) {
                            println("game id: $serverMessage")
                            gameId = serverMessage
                            close(CloseReason(CloseReason.Codes.NORMAL, "ok"))
                            break
                        }
                    }
                }
                gameId!!.toLong()
            }.onFailure {
                println("error accessing ${"ws$SERVER_ADDRESS$USER_API/search-for-game"}")
                it.printStack()
            }
        }
    }

    /**
     * waits for game searching result
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun awaitForGameSearchEnd(): Result<Long>? {
        return searchingForGameJob?.await()
    }
}

/**
 * used to serialize data (position) from the server
 */
@Serializable
class PositionAdapter(
    /**
     * current position
     */
    @Serializable val positions: Array<Boolean?>,
    /**
     * free pieces
     */
    @Serializable val freePieces: Pair<UByte, UByte> = Pair(0U, 0U),
    /**
     * piece to move
     */
    @Serializable val pieceToMove: Boolean,
    /**
     * amount of removals
     */
    @Serializable val removalCount: Byte = 0
)

/**
 * used to serialize data (movement) from the server
 */
@Serializable
class MovementAdapter(
    /**
     * movement start index
     */
    @Serializable val startIndex: Int?,
    /**
     * movement end index
     */
    @Serializable val endIndex: Int?
)

/**
 * Represents the server's response to client requests.
 */
sealed class ServerResponse : Exception() {
    /**
     * Represents a server response indicating that the login is already in use.
     */
    class LoginInUse : ServerResponse()

    /**
     * Represents a server response indicating that the provided password or login is incorrect.
     */
    class WrongPasswordOrLogin : ServerResponse()

    /**
     * this means that server is currently unreachable
     */
    class Unreachable : ServerResponse()

    /**
     * Represents a server response indicating an unknown error.
     */
    class UnknownServerError : ServerResponse()
}

/**
 * Class representing the user's data.
 */
class UserData(
    /**
     * The user's login.
     */
    var login: String,

    /**
     * The user's password.
     */
    var password: String
)
