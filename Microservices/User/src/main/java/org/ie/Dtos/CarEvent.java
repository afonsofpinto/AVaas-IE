package org.ie.Dtos;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;


public class CarEvent {
    String typeEvent;
    Long clientId;
    String carName;
    String manufactor;
    Integer _price;
    Integer price;

    public CarEvent(){}

    public CarEvent(String typeEvent,Long clientId,String carName, String manufactor,Integer price){
        this.typeEvent = typeEvent;
        this.clientId = clientId;
        this.carName = carName;
        this.manufactor = manufactor;
        this.price = price;
    }

    public String getTypeEvent(){ return this.typeEvent;}

    public Long getClientId(){ return this.clientId;}

    public String getCarName(){ return this.carName;}

    public String getManufactor(){ return this.manufactor;}

    public Integer getprice(){ return this.price;}

    @Override
    public String toString() {
        return    "Event: " + typeEvent + " ClientId: " + clientId  + " CarName: " + carName 
                     + "Manufactor: " + manufactor + " Price: " + price + "\n"; 
    }

}
