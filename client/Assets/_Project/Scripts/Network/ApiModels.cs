using System;
using System.Collections.Generic;

namespace DungeonDelvers.Network
{
    /// <summary>REST wrapper: { success, serverTime, data, error } (TDD §7.7).</summary>
    [Serializable]
    public class ApiResponse<T>
    {
        public bool success;
        public long serverTime;
        public T data;
        public ApiError error;
    }

    [Serializable]
    public class ApiError
    {
        public string code;
        public string message;
    }

    [Serializable]
    public class AuthTokens
    {
        public string accessToken;
        public string refreshToken;
        public long expiresIn;
        public string accountId;
    }

    [Serializable]
    public class CharacterDto
    {
        public string id;
        public string name;
        public string classType;
        public int level;
        public long xp;
        public int gold;
    }

    [Serializable]
    public class InventoryDto
    {
        public List<InventoryItemDto> items;
        public int capacity;
        public int gold;
    }

    [Serializable]
    public class InventoryItemDto
    {
        public string id;
        public string itemDefId;
        public string name;
        public string itemType;
        public int quantity;
        public string rarity;
        public int slotIndex;
    }

    /// <summary>Thrown for non-2xx REST responses; <see cref="Code"/> is the server error code.</summary>
    public class ApiException : Exception
    {
        public string Code { get; }
        public long HttpStatus { get; }

        public ApiException(string code, string message, long httpStatus) : base(message)
        {
            Code = code;
            HttpStatus = httpStatus;
        }
    }
}
