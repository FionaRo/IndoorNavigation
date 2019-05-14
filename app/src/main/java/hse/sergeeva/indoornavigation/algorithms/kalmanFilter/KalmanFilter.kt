package hse.sergeeva.indoornavigation.algorithms.kalmanFilter

class KalmanFilter {

    /*these matrices should be provided by user*/
    var F: Matrix //state transition model
    var H: Matrix //observation model
    var B: Matrix //control matrix
    var Q: Matrix //process noise covariance
    var R: Matrix //observation noise covariance

    /*these matrices will be updated by user*/
    var Uk: Matrix //control vector
    var Zk: Matrix //actual values (measured)
    var Xk_km1: Matrix //predicted state estimate
    var Pk_km1: Matrix //predicted estimate covariance
    var Yk: Matrix //measurement innovation

    var Sk: Matrix //innovation covariance
    var SkInv: Matrix //innovation covariance inverse

    var K: Matrix //Kalman gain (optimal)
    var Xk_k: Matrix //updated (current) state
    var Pk_k: Matrix //updated estimate covariance
    var Yk_k: Matrix //post fit residual

    /*auxiliary matrices*/
    var auxBxU: Matrix
    var auxSDxSD: Matrix
    var auxSDxMD: Matrix

    constructor(
        stateDimension: Int,
        measureDimension: Int,
        controlDimension: Int
    ) {
        this.F = Matrix(stateDimension, stateDimension)
        this.H = Matrix(measureDimension, stateDimension)
        this.Q = Matrix(stateDimension, stateDimension)
        this.R = Matrix(measureDimension, measureDimension)

        this.B = Matrix(stateDimension, controlDimension)
        this.Uk = Matrix(controlDimension, 1)

        this.Zk = Matrix(measureDimension, 1)

        this.Xk_km1 = Matrix(stateDimension, 1)
        this.Pk_km1 = Matrix(stateDimension, stateDimension)

        this.Yk = Matrix(measureDimension, 1)
        this.Sk = Matrix(measureDimension, measureDimension)
        this.SkInv = Matrix(measureDimension, measureDimension)

        this.K = Matrix(stateDimension, measureDimension)

        this.Xk_k = Matrix(stateDimension, 1)
        this.Pk_k = Matrix(stateDimension, stateDimension)
        this.Yk_k = Matrix(measureDimension, 1)

        this.auxBxU = Matrix(stateDimension, 1)
        this.auxSDxSD = Matrix(stateDimension, stateDimension)
        this.auxSDxMD = Matrix(stateDimension, measureDimension)
    }

    fun predict() {
        //Xk|k-1 = Fk*Xk-1|k-1 + Bk*Uk
        Matrix.matrixMultiply(F, Xk_k, Xk_km1)
        Matrix.matrixMultiply(B, Uk, auxBxU)
        Matrix.matrixAdd(Xk_km1, auxBxU, Xk_km1)

        //Pk|k-1 = Fk*Pk-1|k-1*Fk(t) + Qk
        Matrix.matrixMultiply(F, Pk_k, auxSDxSD)
        Matrix.matrixMultiplyByTranspose(auxSDxSD, F, Pk_km1)
        Matrix.matrixAdd(Pk_km1, Q, Pk_km1)
    }

    fun update() {
        //Yk = Zk - Hk*Xk|k-1
        Matrix.matrixMultiply(H, Xk_km1, Yk)
        Matrix.matrixSubtract(Zk, Yk, Yk)

        //Sk = Rk + Hk*Pk|k-1*Hk(t)
        Matrix.matrixMultiplyByTranspose(Pk_km1, H, auxSDxMD)
        Matrix.matrixMultiply(H, auxSDxMD, Sk)
        Matrix.matrixAdd(R, Sk, Sk)

        //Kk = Pk|k-1*Hk(t)*Sk(inv)
        if (!Matrix.matrixDestructiveInvert(Sk, SkInv))
            return  //matrix hasn't inversion
        Matrix.matrixMultiply(auxSDxMD, SkInv, K)

        //xk|k = xk|k-1 + Kk*Yk
        Matrix.matrixMultiply(K, Yk, Xk_k)
        Matrix.matrixAdd(Xk_km1, Xk_k, Xk_k)

        //Pk|k = (I - Kk*Hk) * Pk|k-1 - SEE WIKI!!!
        Matrix.matrixMultiply(K, H, auxSDxSD)
        Matrix.matrixSubtractFromIdentity(auxSDxSD)
        Matrix.matrixMultiply(auxSDxSD, Pk_km1, Pk_k)
    }
}