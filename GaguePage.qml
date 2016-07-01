import QtQuick 2.7
import QtQuick.Extras 1.4
import QtQuick.Controls.Styles 1.4

Rectangle {
    color: "black"
    Text {
        anchors.centerIn: parent
        color: "pink"
        font.pointSize: 90
        text: master.throttlePosition
    }

    BaseGauge {
        id: current
        anchors.left: parent.left
        anchors.top: parent.top
        anchors.bottom: parent.bottom
        width: parent.width / 3


        maximumValue: 300
        style: CircularGaugeStyle {
            labelStepSize: 25
            minorTickmarkCount: 0
        }
        value: master.usageCurrent
        unit: value.toFixed(0) + " A"
    }

    BaseGauge {
        id: temp1
        anchors.left: current.right
        anchors.right: parent.horizontalCenter
        anchors.top: parent.top

        maximumValue: 170
        style: CircularGaugeStyle {
            labelStepSize: 20
            minorTickmarkCount: 1
        }
        unit: value.toFixed(0) + " °C"
        value: master.igbt1Temperature
    }

    BaseGauge {
        id: temp2
        anchors.right: speedometer.left
        anchors.left: parent.horizontalCenter
        anchors.top: parent.top

        maximumValue: 170
        style: CircularGaugeStyle {
            labelStepSize: 20
            minorTickmarkCount: 1
        }
        unit: value.toFixed(0) + " °C"
        value: master.igbt2Temperature
    }

    BaseGauge {
        id: speedometer
        anchors.right: parent.right
        anchors.top: parent.top
        anchors.bottom: parent.bottom
        width: parent.width / 3

        maximumValue: 40
        style: CircularGaugeStyle {
            labelStepSize: 5
            minorTickmarkCount: 9
        }
        unit: value.toFixed(0) + " MPH"
        value: master.speed
    }

    Lighting {
        anchors.right: speedometer.left
        anchors.left: current.right
        anchors.top: temp1.bottom
        anchors.bottom: parent.bottom
    }
}