package CarWashSimulation;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

/// ---------------- SEMAPHORE CLASS ----------------
class Semaphore {
    int value;

    public Semaphore(int v) {
        value = v;
    }

    public Semaphore() {
        value = 0;
    }

    public synchronized void P() {
        value--;
        if (value < 0) {
            try {
                wait();
            } catch (InterruptedException ignored) {}
        }
    }

    public synchronized void V() {
        value++;
        if (value <= 0) {
            notify();
        }
    }
}

/// ---------------- BUFFER CLASS ----------------
class Buffer {
    private final Queue<Car> queue;
    private final Semaphore empty, full, mutex, pumps;

    public Buffer(int queueSize, int numberOfPumps) {
        queue = new LinkedList<>();
        empty = new Semaphore(queueSize);
        full = new Semaphore(0);
        mutex = new Semaphore(1);
        pumps = new Semaphore(numberOfPumps);
    }

    // Called by Car (Producer)
    public void produce(Car car) throws InterruptedException {
        System.out.println("🚗 " + car.getName() + " arrived.");
        empty.P();      // Wait for a free spot in the waiting area
        mutex.P();      // Lock queue
        queue.add(car);
        System.out.println("🕓 " + car.getName() + " entered the queue.");
        mutex.V();       // Unlock queue
        full.V();        // Notify pumps that a car is available
    }

    // Called by Pump (Consumer)
    public void consume(int pumpId) throws InterruptedException {
        full.P();       // Wait for an available car
        mutex.P();      // Lock queue
        Car car = queue.poll();
        mutex.V();      // Unlock queue
        empty.V();      // Free a spot in the waiting area

        if (car == null)
            return;

        pumps.P();      // Acquire a bay
        System.out.println("⛽ Pump " + pumpId + " started servicing " + car.getName());
        Thread.sleep((int) (Math.random() * 3000 + 1000));
        System.out.println("✅ Pump " + pumpId + " finished servicing " + car.getName());
        pumps.V();      // Release the bay
    }
}

/// ---------------- CAR (PRODUCER) ----------------
class Car extends Thread {
    private final Buffer buffer;

    public Car(String name, Buffer buffer) {
        super(name);
        this.buffer = buffer;
    }

    @Override
    public void run() {
        try {
            buffer.produce(this);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

/// ---------------- PUMP (CONSUMER) ----------------
class Pump extends Thread {
    private final int pumpId;
    private final Buffer buffer;

    public Pump(int id, Buffer buffer) {
        this.pumpId = id;
        this.buffer = buffer;
    }

    @Override
    public void run() {
        while (true) {
            try {
                buffer.consume(pumpId);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}

/// ---------------- MAIN CLASS ----------------
public class ServiceStation {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter waiting area size (1–10): ");
        int waitingAreaSize = scanner.nextInt();
        System.out.print("Enter number of pumps: ");
        int numberOfPumps = scanner.nextInt();
        System.out.print("Enter number of cars: ");
        int numberOfCars = scanner.nextInt();

        if (waitingAreaSize < 1 || waitingAreaSize > 10) {
            System.out.println("⚠️ Queue size must be between 1 and 10.");
            return;
        }

        Buffer buffer = new Buffer(waitingAreaSize, numberOfPumps);

        // Start pumps (consumers)
        for (int i = 1; i <= numberOfPumps; i++) {
            new Pump(i, buffer).start();
        }

        // Start cars (producers)
        for (int i = 1; i <= numberOfCars; i++) {
            new Car("C" + i, buffer).start();
            try {
                Thread.sleep(500); // simulate arrival delay
            } catch (InterruptedException ignored) {}
        }

        // Optional: simulation timeout
        try {
            Thread.sleep(6000);
            System.out.println("\nAll cars processed; simulation ends.");
            System.exit(0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}