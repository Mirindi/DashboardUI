#include "Master.hpp"
#include "Fixed16Dataset.hpp"
#include "Fixed32Dataset.hpp"
#include "Fixed32AngleDataset.hpp"

#ifdef Q_OS_ANDROID
#include <QAndroidJniEnvironment>
#include <QAndroidJniObject>
#endif

Master* Master::instance_ = nullptr;

#define MOTOR_CONTROLLER_DUTY_CYCLE_SID 1398608173ul
#define MOTOR_CONTROLLER_IGBT1_TEMPERATURE_SID 0x82046B5Cul
#define MOTOR_CONTROLLER_IGBT2_TEMPERATURE_SID 0xF4B0B5FDul
//#define HEAD_LIGHT_STATE_SID 999653166ul
//#define SIGNAL_LIGHT_STATE_SID 2308980954ul
#define AXLE_RPM_SID 0xD211EF5Dul
#define BATTERY_VOLTAGE_SID 4052165617ul
#define USAGE_CURRENT_SID 0x5A23E81Cul
#define CHARGING_CURRENT_SID 3484793322ul

#define MOTOR_THETA_SID 0xFF76A82Bul

#define Q_AXIS_CURRENT_SID 0xBF79A32Cul

#define COMMANDED_Q_AXIS_CURRENT_SID 0x6E01A3B3ul

#define D_AXIS_CURRENT_SID 0xBF79A31Ful

#define COMMANDED_D_AXIS_CURRENT_SID 0xA84C6F66ul

Master::Master(QObject* parent)
    : QAbstractListModel(parent)
    , batteryVoltage_(0)
    , usageCurrent_(0)
    , throttlePosition_(0)
    , igbt1Temperature_(0)
    , igbt2Temperature_(0)
    , batteryLife_(0)
    , speed_(0)
    , signalLightState_(0)
    , headLightState_(0)
{
    start_ = std::chrono::high_resolution_clock::now();
    instance_ = this;
    //datasets_.push_back(new UFixed16Dataset{ "Throttle Position", "%", MOTOR_CONTROLLER_DUTY_CYCLE_SID, this });

    datasets_.push_back(new Fixed32AngleDataset{ "Electrical Angle", MOTOR_THETA_SID, this });

    datasets_.push_back(new Fixed32Dataset{ "q-Axis Current", "A", Q_AXIS_CURRENT_SID, this });
    datasets_.push_back(new Fixed32Dataset{ "Commanded q-Axis Current", "A", COMMANDED_Q_AXIS_CURRENT_SID, this });

    datasets_.push_back(new Fixed32Dataset{ "d-Axis Current", "A", D_AXIS_CURRENT_SID, this });
    datasets_.push_back(new Fixed32Dataset{ "Commanded d-Axis Current", "A", COMMANDED_D_AXIS_CURRENT_SID, this });

    /*datasets_.push_back(new N16Dataset{"Battery Voltage",BATTERY_VOLTAGE_SID,this});
    datasets_.push_back(new N16Dataset{"Battery Current",USAGE_CURRENT_SID,this});
    datasets_.push_back(new N16Dataset{"IGBT1 Temperature",MOTOR_CONTROLLER_IGBT1_TEMPERATURE_SID,this});
    datasets_.push_back(new N16Dataset{"IGBT2 Temperature",MOTOR_CONTROLLER_IGBT2_TEMPERATURE_SID,this});
    datasets_.push_back(new N16Dataset{"Battery Life",BATTERY_VOLTAGE_SID,this});
    datasets_.push_back(new N16Dataset{"Speed",AXLE_RPM_SID,this});*/
}

Master::~Master()
{
    qDeleteAll(datasets_);
}

Master* Master::instance()
{
    return instance_;
}

double Master::time() const
{
    return std::chrono::duration_cast<std::chrono::duration<double, std::milli> >(std::chrono::high_resolution_clock::now() - start_).count();
}

double Master::batteryVoltage() const
{
    return batteryVoltage_;
}

void Master::setBatteryVoltage(double batteryVoltage)
{
    if (batteryVoltage_ != batteryVoltage) {
        batteryVoltage_ = batteryVoltage;
        emit batteryVoltageChanged(batteryVoltage_);
    }
}

double Master::usageCurrent() const
{
    return usageCurrent_;
}

void Master::setUsageCurrent(double usageCurrent)
{
    if (usageCurrent_ != usageCurrent) {
        usageCurrent_ = usageCurrent;
        emit usageCurrentChanged(usageCurrent);
    }
}

double Master::throttlePosition() const
{
    return throttlePosition_;
}

void Master::setThrottlePosition(double throttlePosition)
{
    if (throttlePosition_ != throttlePosition) {
        throttlePosition_ = throttlePosition;
        emit throttlePositionChanged(throttlePosition);
    }
}

double Master::igbt1Temperature() const
{
    return igbt1Temperature_;
}

void Master::setIgbt1Temperature(double igbtTemperature)
{
    if (igbt1Temperature_ != igbtTemperature) {
        igbt1Temperature_ = igbtTemperature;
        emit igbt1TemperatureChanged(igbtTemperature);
    }
}

double Master::igbt2Temperature() const
{
    return igbt2Temperature_;
}

void Master::setIgbt2Temperature(double igbt2Temperature)
{
    if (igbt2Temperature_ != igbt2Temperature) {
        igbt2Temperature_ = igbt2Temperature;
        emit igbt2TemperatureChanged(igbt2Temperature);
    }
}

double Master::batteryLife() const
{
    return batteryLife_;
}

void Master::setBatteryLife(double batteryLife)
{
    if (batteryLife_ != batteryLife) {
        batteryLife_ = batteryLife;
        emit batteryLifeChanged(batteryLife);
    }
}

double Master::speed() const
{
    return speed_;
}

void Master::setSpeed(double speed)
{
    if (speed_ != speed) {
        speed_ = speed;
        emit speedChanged(speed);
    }
}

int Master::signalLightState() const
{
    return signalLightState_;
}

void Master::setSignalLightState(int signalLightState)
{
    if (signalLightState_ != signalLightState) {
        signalLightState_ = signalLightState;
#ifdef Q_OS_ANDROID
        QAndroidJniObject::callStaticMethod<void>("com/ptransportation/FullscreenActivity",
            "sendSignalLightState",
            "(I)V",
            signalLightState_);
#endif
        emit signalLightStateChanged(signalLightState);
    }
}

int Master::headLightState() const
{
    return headLightState_;
}

void Master::setHeadLightState(int headLightState)
{
    if (headLightState_ != headLightState) {
        headLightState_ = headLightState;
#ifdef Q_OS_ANDROID
        QAndroidJniObject::callStaticMethod<void>("com/ptransportation/FullscreenActivity",
            "sendHeadLightState",
            "(I)V",
            headLightState_);
#endif
        emit headLightStateChanged(headLightState);
    }
}

void Master::signalReceived(uint32_t SID, uint8_t* data, uint8_t length)
{
    for (auto ds : datasets_)
        ds->onSignalReceived(SID, data, length);
}

QHash<int, QByteArray> Master::roleNames() const
{
    QHash<int, QByteArray> roles;
    roles[NameRole] = "name";
    return roles;
}

int Master::rowCount(const QModelIndex& parent) const
{
    return datasets_.size();
}

QVariant Master::data(const QModelIndex& index, int role) const
{
    if (index.row() < 0 || index.row() >= datasets_.size()) {
        return QVariant();
    }

    auto dataset = datasets_[index.row()];

    if (role == NameRole) {
        return QVariant::fromValue(dataset->name());
    }

    return QVariant();
}

Dataset* Master::getDataset(int index)
{
    return datasets_[index];
}
