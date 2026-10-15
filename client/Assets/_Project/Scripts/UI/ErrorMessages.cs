namespace DungeonDelvers.UI
{
    /// <summary>Player-facing text for server error codes (network-protocol §3, REST ErrorCode).</summary>
    public static class ErrorMessages
    {
        public static string ForCode(string code, string fallback = null) => code switch
        {
            "INVENTORY_FULL" => "Túi đầy! Không nhặt được đồ",
            "RATE_LIMITED" => "Thao tác quá nhanh, thử lại sau",
            "SKILL_ON_COOLDOWN" => "Kỹ năng đang hồi",
            "INSUFFICIENT_MP" => "Không đủ MP",
            "TARGET_INVALID" => "Mục tiêu không hợp lệ hoặc ở quá xa",
            "NOT_IN_ZONE" => "Bạn không ở trong phòng",
            "INSTANCE_NOT_FOUND" => "Phòng không còn tồn tại",
            "UNAUTHORIZED" => "Phiên đăng nhập hết hạn",
            "MAINTENANCE" => "Server đang bảo trì",
            "NETWORK_ERROR" => "Không kết nối được server",
            "INVALID_MESSAGE" or "INVALID_INPUT" => "Dữ liệu không hợp lệ" + (fallback != null ? ": " + fallback : ""),
            "EMAIL_EXISTS" => "Email đã được đăng ký",
            "NAME_TAKEN" => "Tên nhân vật đã có người dùng",
            "INTERNAL_ERROR" => "Lỗi server, thử lại sau",
            _ => fallback ?? code
        };
    }
}
