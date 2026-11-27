package br.com.fiec.helptec.network

/**
 * Endereço do backend em UM lugar só.
 *  - Emulador Android apontando para o PC: http://10.0.2.2:8080/
 *  - Celular físico na mesma rede: http://<IP do PC>:8080/
 *  - Kubernetes (minikube): http://<IP do nó>:30080/
 * Precisa terminar com "/".
 */
object ApiConfig {
    const val BASE_URL = "https://o3l1ibqnmb.execute-api.us-east-1.amazonaws.com/helptec/"
}
