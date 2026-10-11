package rw.afriteck.wms.auth.dtos;

public record TokenResponse(String accessToken,
                            String tokenType,            // "Bearer"
                            long expiresIn,              // seconds
                            String refreshToken,
                            long refreshExpiresIn,       // seconds
                            boolean mustChangePassword,
                            UserResponse user) {}
