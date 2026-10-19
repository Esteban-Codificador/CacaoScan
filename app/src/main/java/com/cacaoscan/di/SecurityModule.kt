package com.cacaoscan.di

import android.content.Context
import com.cacaoscan.security.SecuritySession
import com.cacaoscan.security.authz.LocalTokenService
import com.cacaoscan.security.authz.TokenSigner
import com.cacaoscan.security.credentials.AdminCredentialStore
import com.cacaoscan.security.credentials.ByteCipher
import com.cacaoscan.security.credentials.DefaultAdminCredentialStore
import com.cacaoscan.security.credentials.KeyValueStore
import com.cacaoscan.security.credentials.PreferencesKeyValueStore
import com.cacaoscan.security.keystore.KeystoreCipher
import com.cacaoscan.security.keystore.KeystoreHmacSigner
import com.cacaoscan.security.lock.AppLock
import com.cacaoscan.security.lock.BiometricAppLock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * Modulo de seguridad local.
 *
 * Todo lo que se declara aqui es opcional respecto al flujo principal: si se
 * elimina este modulo, CacaoScan sigue capturando, clasificando y guardando
 * revisiones. Esa independencia es deliberada y esta documentada en
 * /docs/06-seguridad-y-autorizacion.md
 */
@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideTokenSigner(): TokenSigner = KeystoreHmacSigner()

    @Provides
    @Singleton
    fun provideLocalTokenService(signer: TokenSigner, clock: Clock): LocalTokenService =
        LocalTokenService(signer, clock)

    @Provides
    @Singleton
    fun provideSecuritySession(tokenService: LocalTokenService): SecuritySession =
        SecuritySession(tokenService)

    @Provides
    @Singleton
    fun provideAppLock(@ApplicationContext context: Context): AppLock =
        BiometricAppLock(context)

    @Provides
    @Singleton
    fun provideKeyValueStore(@ApplicationContext context: Context): KeyValueStore =
        PreferencesKeyValueStore(context)

    /** Clave AES del Keystore dedicada a proteger la credencial de administrador. */
    @Provides
    @Singleton
    fun provideCredentialCipher(): ByteCipher = KeystoreCipher(CREDENTIAL_KEY_ALIAS)

    @Provides
    @Singleton
    fun provideAdminCredentialStore(
        store: KeyValueStore,
        cipher: ByteCipher,
        clock: Clock,
    ): AdminCredentialStore = DefaultAdminCredentialStore(store, cipher, clock)

    private const val CREDENTIAL_KEY_ALIAS = "cacaoscan_admin_credential"
}
