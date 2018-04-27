package com.app.dtu.bean.model.device;

import com.app.dtu.bean.DataMsg;
import com.app.dtu.bean.Message;
import com.app.dtu.bean.model.*;
import com.app.dtu.config.DtuConfig;
import com.app.dtu.service.ServiceItem;
import com.app.dtu.util.DtuUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.util.List;
import java.util.Objects;

/**
 * 烟感设备
 */

@Entity
@Table(name =  DtuConfig.DTU_TABLE_PRIFIX +"smoke_feel_monitor_device")
public class SmokeFeelMonitorDevice extends RedundancyDeviceData implements DeviceDataDeal, ParseToEntityAdapter<SmokeFeelMonitorDevice> {
    private static final Logger logger = LoggerFactory.getLogger(SmokeFeelMonitorDevice.class);

    public SmokeFeelMonitorDevice(Message message) {
        setMessage(message);
    }

    public SmokeFeelMonitorDevice() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer pt;
    private Integer y1;

    @Override
    public SmokeFeelMonitorDevice generateEntity(Message message) {
        buildRedunancyDeviceInfo();
        for (int i = 0; i < message.getDataMsgs().size(); i++) {
            DataMsg dataMsg = message.getDataMsgs().get(i);
            List<Integer> dataMsgs = dataMsg.getDatas();
            if (message.parseDeviceModelEnum() == DeviceTypeName.SMOKE_FEE_MONITOR_0501) {
                if (DataType.getValue(dataMsg.getType()) == DataType.DATA_TYPE_02) {
                    pt = DtuUtil.getValue(dataMsgs, 0);
                } else if (DataType.getValue(dataMsg.getType()) == DataType.DATA_TYPE_06) {
                    y1 = DtuUtil.getValue(dataMsgs, 0);
                }
            }
        }
        return this;
    }

    @Override
    public void parseDeviceStatus() {

    }

    @Override
    public boolean execute() {
        try{
            DeviceDataDeal deviceDataDeal = getStorageEntity();
            if (Objects.isNull(deviceDataDeal)){
                return false;
            }
            ServiceItem.somkeFeeService.save(deviceDataDeal);
        }catch (Throwable e){
            logger.error("Execute add data to db or generate entity is error");
        }
        return true;
    }

    @Override
    public SmokeFeelMonitorDevice buildDevice() {
        return this;
    }

    @Override
    public Message buildMessage() {
        return getMessage();
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public static Logger getLogger() {
        return logger;
    }

    public Integer getPt() {
        return pt;
    }

    public void setPt(Integer pt) {
        this.pt = pt;
    }

    public Integer getY1() {
        return y1;
    }

    public void setY1(Integer y1) {
        this.y1 = y1;
    }

    @Override
    public String toString() {
        return "SmokeFeelMonitorDevice{" +
                "id=" + id +
                ", pt=" + pt +
                ", y1=" + y1 +
                '}';
    }
}
