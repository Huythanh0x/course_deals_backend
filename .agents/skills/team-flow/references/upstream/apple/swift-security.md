---
name: swift-security
description: Apple platform security rules for Course Deals (`iosApp/` & `watchApp/`): Keychain Services (`SecItemAdd`, `SecItemUpdate`, `OSStatus`), biometric/Passkey auth (`ASAuthorizationController`, `LAContext`), `PrivacyInfo.xcprivacy`, and OWASP MASVS compliance.
source: https://github.com/dpearson2699/swift-ios-skills/blob/main/skills/swift-security/SKILL.md (Aligned to Course Deals)
---

# Course Deals Apple Security: Keychain, Passkeys & Privacy Manifests

Aligned to `course_deals_ComposeMultiplatform` (`iosApp/`, `watchApp/`, and `data:user` `iosMain`).

## 1. Security Invariants for Credentials & Tokens

- **Never store JWT access tokens, refresh tokens, Passkey credentials, or API secrets in `UserDefaults`, `Info.plist`, `.xcconfig`, or source code.**
  - Note: Non-sensitive user preferences (theme, onboarding flags) may live in `SettingsStore` (`NSUserDefaults`), while auth tokens belong in Keychain with `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`.
- **Always check every `OSStatus`** from `SecItemAdd`, `SecItemCopyMatching`, `SecItemUpdate`, and `SecItemDelete`.
- **Add-or-Update Pattern:** Never use delete-then-add for routine Keychain updates; handle `errSecDuplicateItem` from `SecItemAdd` by calling `SecItemUpdate`:
  ```swift
  func saveToken(_ data: Data, account: String) throws {
      let identity: [CFString: Any] = [
          kSecClass: kSecClassGenericPassword,
          kSecAttrService: "com.thanh0x.coursedeals.auth",
          kSecAttrAccount: account,
      ]
      var add = identity
      add[kSecValueData] = data
      add[kSecAttrAccessible] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly

      switch SecItemAdd(add as CFDictionary, nil) {
      case errSecSuccess:
          return
      case errSecDuplicateItem:
          let status = SecItemUpdate(
              identity as CFDictionary,
              [kSecValueData: data] as CFDictionary
          )
          guard status == errSecSuccess else { throw KeychainError(status: status) }
      case let status:
          throw KeychainError(status: status)
      }
  }
  ```

## 2. Biometrics & Passkeys (`ASAuthorizationController`)

- Never gate a secret solely behind a boolean `LAContext.evaluatePolicy()` check in memory; bind biometric-protected items using `SecAccessControlCreateWithFlags(..., .biometryCurrentSet, ...)`.
- For Sign in with Apple and WebAuthn Passkeys (integrating with `course_deals_backend` `modules:identity-service`), use `ASAuthorizationController` with `ASAuthorizationPlatformPublicKeyCredentialProvider` and `ASAuthorizationAppleIDProvider`.

## 3. Privacy Manifest (`PrivacyInfo.xcprivacy`)

- Declare required-reason APIs (such as `NSPrivacyAccessedAPICategoryUserDefaults` with reason `CA92.1` when `NSUserDefaults` is used by `SettingsStore`) in `PrivacyInfo.xcprivacy`.

## Checklist
- [ ] Auth tokens/secrets stored in Keychain (`kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`), never plain `UserDefaults`
- [ ] All `SecItem*` calls handle `OSStatus` (`errSecSuccess`, `errSecDuplicateItem`, `errSecItemNotFound`)
- [ ] Logout clears both Keychain credentials and `SettingsStore` user session state
- [ ] `PrivacyInfo.xcprivacy` declares required-reason APIs (`CA92.1` for `UserDefaults`)
