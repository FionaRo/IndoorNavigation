package hse.sergeeva.indoornavigation.models

class TowerStatistics {
    companion object {
        var allTowers: Int = 0
        var foundTowersOpenCellId: Int = 0
        var notFoundTowersOpenCellId: Int = 0
        var foundTowersMylnikov: Int = 0
        var notFoundTowersMylnikov: Int = 0
        var duplicateCellId: Int = 0
        var duplicateMyLinkov: Int = 0
    }
}