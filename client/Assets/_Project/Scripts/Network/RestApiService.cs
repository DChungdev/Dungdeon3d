using System.Collections.Generic;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;
using UnityEngine.Networking;

namespace DungeonDelvers.Network
{
    /// <summary>
    /// REST calls to the game server. Must be called from the main thread (UnityWebRequest).
    /// Keeps the current tokens; <see cref="RefreshAsync"/> rotates them.
    /// </summary>
    public class RestApiService
    {
        private const int TimeoutSec = 10;

        public AuthTokens Tokens { get; private set; }

        public async Task<AuthTokens> RegisterAsync(string email, string password)
        {
            Tokens = await SendAsync<AuthTokens>("POST", "/api/v1/auth/register",
                new { email, password }, auth: false);
            return Tokens;
        }

        public async Task<AuthTokens> LoginAsync(string email, string password)
        {
            Tokens = await SendAsync<AuthTokens>("POST", "/api/v1/auth/login",
                new { email, password }, auth: false);
            return Tokens;
        }

        /// <summary>Gets a fresh access token (refresh tokens are single-use on the server).</summary>
        public async Task<AuthTokens> RefreshAsync()
        {
            Tokens = await SendAsync<AuthTokens>("POST", "/api/v1/auth/refresh",
                new { refreshToken = Tokens.refreshToken }, auth: false);
            return Tokens;
        }

        public Task<List<CharacterDto>> ListCharactersAsync() =>
            SendAsync<List<CharacterDto>>("GET", "/api/v1/characters", null, auth: true);

        public Task<InventoryDto> GetInventoryAsync(string characterId) =>
            SendAsync<InventoryDto>("GET", "/api/v1/characters/" + characterId + "/inventory", null, auth: true);

        public Task<CharacterDto> CreateCharacterAsync(string name, string classType) =>
            SendAsync<CharacterDto>("POST", "/api/v1/characters", new { name, classType }, auth: true);

        private async Task<T> SendAsync<T>(string method, string path, object body, bool auth)
        {
            using var request = new UnityWebRequest(ServerConfig.RestBaseUrl + path, method)
            {
                downloadHandler = new DownloadHandlerBuffer(),
                timeout = TimeoutSec
            };
            if (body != null)
            {
                request.uploadHandler = new UploadHandlerRaw(Encoding.UTF8.GetBytes(JsonConvert.SerializeObject(body)));
                request.SetRequestHeader("Content-Type", "application/json");
            }
            if (auth && Tokens != null)
            {
                request.SetRequestHeader("Authorization", "Bearer " + Tokens.accessToken);
            }

            var completion = new TaskCompletionSource<bool>();
            request.SendWebRequest().completed += _ => completion.SetResult(true);
            await completion.Task;

            if (request.result == UnityWebRequest.Result.ConnectionError)
            {
                throw new ApiException("NETWORK_ERROR", "Cannot reach server: " + request.error, 0);
            }

            ApiResponse<T> response = null;
            try
            {
                response = JsonConvert.DeserializeObject<ApiResponse<T>>(request.downloadHandler.text);
            }
            catch (JsonException)
            {
                // fall through to the generic error below
            }
            if (response == null || !response.success)
            {
                throw new ApiException(
                    response?.error?.code ?? "HTTP_" + request.responseCode,
                    response?.error?.message ?? request.error ?? "Unexpected response",
                    request.responseCode);
            }
            return response.data;
        }
    }
}
