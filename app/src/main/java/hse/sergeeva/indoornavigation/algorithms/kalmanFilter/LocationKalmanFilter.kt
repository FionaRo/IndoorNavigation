package hse.sergeeva.indoornavigation.algorithms.kalmanFilter

class LocationKalmanFilter {

    private var kalmanFilter: KalmanFilter
    private var stampMsPredict: Double = 0.0
    private var stampMsUpdate: Double = 0.0
    private var accSigma: Double = 0.0
    private var predictCount: Int = 0
    private var velFactor = 1.0
    private var posFactor = 1.0

    constructor(
        x: Double, y: Double,
        xVel: Double, yVel: Double,
        accDev: Double, posDev: Double,
        timeStampMs: Double, velFactor: Double,
        posFactor: Double
    ) {
        val mesDim = 2

        kalmanFilter = KalmanFilter(4, mesDim, 1)
        stampMsUpdate = timeStampMs
        stampMsPredict = timeStampMs
        accSigma = accDev
        predictCount = 0
        kalmanFilter.Xk_k.setData(doubleArrayOf(x, y, xVel, yVel))

        kalmanFilter.H.setIdentityDiag() //state has 4d and measurement has 4d too. so here is identity
        kalmanFilter.Pk_k.setIdentity()
        kalmanFilter.Pk_k.scale(posDev)
        this.velFactor = velFactor
        this.posFactor = posFactor
    }

    private fun rebuildF(dtPredict: Double) {
        val f = doubleArrayOf(
            1.0, 0.0, dtPredict, 0.0,
            0.0, 1.0, 0.0, dtPredict,
            0.0, 0.0, 1.0, 0.0,
            0.0, 0.0, 0.0, 1.0
        )
        kalmanFilter.F.setData(f)
    }

    private fun rebuildU(xAcc: Double, yAcc: Double) {
        kalmanFilter.Uk.setData(doubleArrayOf(xAcc, yAcc))
    }

    private fun rebuildB(dtPredict: Double) {
        val dt2 = 0.5 * dtPredict * dtPredict
        val b = doubleArrayOf(
            dt2, 0.0,
            0.0, dt2,
            dtPredict, 0.0,
            0.0, dtPredict
        )
        kalmanFilter.B.setData(b)
    }

    private fun rebuildR(posSigma_: Double, velSigma_: Double) {
        var posSigma = posSigma_
        var velSigma = velSigma_

        posSigma *= posFactor
        velSigma *= velFactor

        kalmanFilter.R.setIdentity()
        kalmanFilter.R.scale(posSigma)
    }

    private fun rebuildQ(dtUpdate: Double, accDev: Double) {
        val velDev = accDev * predictCount
        val posDev = velDev * predictCount / 2
        val covDev = velDev * posDev

        val posSig = posDev * posDev
        val velSig = velDev * velDev

        val Q = doubleArrayOf(
            posSig, 0.0, covDev, 0.0,
            0.0, posSig, 0.0, covDev,
            covDev, 0.0, velSig, 0.0,
            0.0, covDev, 0.0, velSig
        )
        kalmanFilter.Q.setData(Q)
    }

    fun predict(timeNowMs: Double, xAcc: Double, yAcc: Double) {
        val dtPredict = (timeNowMs - stampMsPredict) / 1000.0
        val dtUpdate = (timeNowMs - stampMsUpdate) / 1000.0
        rebuildF(dtPredict)
        rebuildB(dtPredict)
        rebuildU(xAcc, yAcc)

        ++predictCount
        rebuildQ(dtUpdate, accSigma)

        stampMsPredict = timeNowMs
        kalmanFilter.predict()
        Matrix.matrixCopy(kalmanFilter.Xk_km1, kalmanFilter.Xk_k)
    }

    fun update(
        timeStamp: Double, x: Double, y: Double,
        xVel: Double, yVel: Double, posDev: Double, velErr: Double
    ) {
        predictCount = 0
        stampMsUpdate = timeStamp
        rebuildR(posDev, velErr)
        kalmanFilter.Zk.setData(doubleArrayOf(x, y, xVel, yVel))
        kalmanFilter.update()
    }

    fun getCurrentX() = kalmanFilter.Xk_k.data[0][0]
    fun getCurrentY() = kalmanFilter.Xk_k.data[1][0]
    fun getCurrentXVel() = kalmanFilter.Xk_k.data[2][0]
    fun getCurrentYVel() = kalmanFilter.Xk_k.data[3][0]
}