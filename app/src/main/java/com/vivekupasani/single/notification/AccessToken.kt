import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

object AccessToken {

    val firebaseMessagingScope = "https://www.googleapis.com/auth/firebase.messaging"

    suspend fun getAccessToken(): String? {
        return withContext(Dispatchers.IO) {  // Switch to IO thread for network operation
            try {
                val jsonString = "{\n" +
                        "  \"type\": \"service_account\",\n" +
                        "  \"project_id\": \"single-5664b\",\n" +
                        "  \"private_key_id\": \"3e39bebda14a13101877b226a81f717d80caf99a\",\n" +
                        "  \"private_key\": \"-----BEGIN PRIVATE KEY-----\\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC0/jQR9iczs+ZH\\nyukQmn5RToBAWKaAjgwqXWqhEt4SKTFVp43clwk+qLGEZEj7ftu5UCDxzQ8qImA2\\njSAC0PvZsyJNUYAD7K4qPPUzMOTBqP8ocjajLCS+IdgA6ih+xo1TKVMBGNv2mDix\\nx+FuKtQP6b04HiEbybfZ6b7aJaR3Tbu5VLOgIlRgQVGXt1g9A3y+kwoYjPICQZ6M\\nm2wcyFhLmGMu8Lz8fXOZOuTT6xhj/mtKfw96H6VkRZRnWbt9/xk07xXsohn+EsaJ\\nHSE2SyS2n5AdTcocizIddFgoXG/TWvXGm2R/AGyNGhgck2Diz14C2B9jN0cnXuyV\\nNw8MNM4DAgMBAAECggEABICEOWQm59Apl2SSiowFPgQsCC22/+Cd/dnIArdY7cO3\\ni0Vj2nZT+l/bgTW2DlzCnhKXrIa+F1K2k592tIs6kIOLrrerGFiP9n4ArWQhyeK9\\nwhBL5UrTo8/CCI0f3RNsRYLJd/AEh1bpphvDiD1+0TcuXTx79K3Tqb3ONlfpsOFO\\njeG6EahkexjLKnkCPUTYXoNEEJpkuRzxlx87gW22V5atY79vbl/wJxpw30Dj/Dfp\\nh1ZM3Ka2krMZe+vDYNi0P47XmDMJBcjGQ2O9Kk6KVvEdmgcu7LTeg5tM8zVbRLH2\\nbO769OVUZrpCRI8tmmnAd4tDZmrLfJXMdkjI/cWQ2QKBgQDjqJvmI9X9wFcXnLd2\\nSm+TdVJ5Jx6dqZG8WaKlbzSBLMBc+Ksfz9BmsMG8rwp3pllyDbU1JoPyCbRV+NjZ\\nrMzAKrqm7z/hpcuIdmWsMcLEbISTdVIduIEY0g9y5AcBIUAoggA1VV9GLJDj3WtB\\n6CLhux5M978swGfR63iZ2a5IyQKBgQDLhmDru9ObGGzyTXhTE6xwpmhWVfFNHyHV\\n/fkEjWcRXGZvn4q1klO3jQJptBu3NNpuy2F4FH4rDrdtiklmeMv+6RuSPRGfyJEl\\n6mjZsuR04/wRzseEhSedG0O5qTWcBQLjAhADL1e2r+TjttQ8kRZ0EVP29JYIwQ+Y\\nSAbcpqxSawKBgBCeTkMOeqB7WeAvYHUSGfL5rXuKj4GSz0Cgim7pHzwOuDwRKuy3\\njs7wODQ90tXJHt5kgcXPefJBIxjjgXDiXE/qzpVBAbjHEYR0oBdhyoXsJgFdnxDs\\nVwAo1VfdrWU3uCc81icOhFXrCuiXUS0OdP1lW2Di9OGfafS466TPbxmBAoGBAI9R\\nk5Ks83f2HVILZ73ozreNF1AkuQV79NHCRMcF5Bx/msrj6EOwRS+uJpLU9+dtWpxG\\nr95lX+tmM5j5lnKIge6BrJ6wTmbcUAoJJciXhvUhTnIj0K/rLFgfyPNGyq+Rf9Tn\\nsOrtPbEx79bC+nWkHdGiGiR/W9/SQ+8SWXKsFBD9AoGBAOGuGz9gNatqBY1Tf6ps\\nD7XVQ3uoo77uheftqjhLKTtKGgE3SNAwKncQMVomDp/Fz/+10+xBTx6M5DBfNNRB\\ndOWsQOI1/z/SFjlYZU6tDzrhR3/WjTi5MTYuiW9WQKLL9HVxfVsR6YxazbFanY3I\\nfXjbLbTNFDThSfSMmLiiZx+h\\n-----END PRIVATE KEY-----\\n\",\n" +
                        "  \"client_email\": \"firebase-adminsdk-5cosp@single-5664b.iam.gserviceaccount.com\",\n" +
                        "  \"client_id\": \"116493912110552369161\",\n" +
                        "  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n" +
                        "  \"token_uri\": \"https://oauth2.googleapis.com/token\",\n" +
                        "  \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n" +
                        "  \"client_x509_cert_url\": \"https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-5cosp%40single-5664b.iam.gserviceaccount.com\",\n" +
                        "  \"universe_domain\": \"googleapis.com\"\n" +
                        "}\n"

                val stream = ByteArrayInputStream(jsonString.toByteArray(StandardCharsets.UTF_8))

                val googleCredential = GoogleCredentials.fromStream(stream)
                    .createScoped(arrayListOf(firebaseMessagingScope))

                googleCredential.refresh()

                googleCredential.accessToken.tokenValue
            } catch (e: IOException) {
                null
            }
        }
    }
}
