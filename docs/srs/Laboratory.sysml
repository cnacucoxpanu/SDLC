
package LaboratoryEquipmentManagement {

    part def LaboratoryTechnician {
        attribute responsibilities : String =
            "Manage equipment catalog; change status; view all bookings";
    }

    part def Student {
        attribute responsibilities : String =
            "Search equipment; reserve for a period; cancel own bookings";
    }

    part def SwingInterface {
        attribute components : String =
            "Login window, catalog, forms, booking tables";
    }

    part def ApplicationServices {
        part authService;
        part equipmentService;
        part bookingService;
    }

    part def AuthService;
    part def EquipmentService;
    part def BookingService;

    part def Equipment {
        attribute inventoryNumber : String;
        attribute name : String;
        attribute category : String;
        attribute room : String;
        attribute status : EquipmentStatus;
    }

    enum def EquipmentStatus {
        enum free;
        enum underRepair;
    }

    part def Booking {
        attribute studentId : String;
        attribute equipmentId : String;
        attribute startDate : String;
        attribute endDate : String;
        attribute status : BookingStatus;
    }

    enum def BookingStatus {
        enum active;
        enum cancelled;
    }

    part def FileStorage {
        attribute files : String =
            "users.tsv, equipment.tsv, bookings.tsv";
    }

    part def LaboratoryEquipmentSystem {
        part technician : LaboratoryTechnician;
        part student : Student;
        part ui : SwingInterface;

        part services : ApplicationServices {
            part authService : AuthService;
            part equipmentService : EquipmentService;
            part bookingService : BookingService;
        }

        part equipment : Equipment;
        part booking : Booking;
        part database : FileStorage;
    }

    connection def UsesService {
        end part client;
        end part service;
    }

    connection def ServiceAccess {
        end part service;
        end part resource;
    }

    connection def StoresBookings {
        end part booking;
        end part storage;
    }

    part system : LaboratoryEquipmentSystem {
        connection technicianAccess : UsesService
            connect technician to services;

        connection studentAccess : UsesService
            connect student to services;

        connection uiAccess : UsesService
            connect ui to services;

        connection equipmentAccess : ServiceAccess
            connect services to equipment;

        connection bookingAccess : ServiceAccess
            connect services to booking;

        connection bookingStorage : StoresBookings
            connect booking to database;
    }

    requirement def <'REQ-FUNC-005'> RepairCancelsBookings {
        doc /* Transferring equipment to repair cancels
               all active bookings for that equipment. */
    }

    requirement def <'REQ-FUNC-006'> BookingPeriodLimit {
        doc /* A booking period must not exceed 7 days,
               and bookings for the same equipment must not overlap. */
    }
}
