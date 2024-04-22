package com.example.calculatorwsclient.api;

import com.example.calculatorwsclient.service.CalculatorWebService;
import com.example.calculatorwsclient.service.CalculatorWebServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.rmi.RemoteException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@RestController
public class CalculatorController {
    @Autowired
    CalculatorWebService calculatorWebService;

    private final ExecutorService virtualThreadExecutorService;

    @Autowired
    public CalculatorController(ExecutorService virtualThreadExecutorService) {
        this.virtualThreadExecutorService = virtualThreadExecutorService;
    }

    //    @GetMapping("/async/vt/test")
//    public Mono<Integer>  asyncVirtualThreadSum() throws Exception {
//        CompletableFuture<Integer> futureResult = calculatorWebService.performAsyncAddOperation(1, 2);
//        return Mono.fromFuture(futureResult);
//
//    }
    @GetMapping("/async/test")
    public Integer asyncSum() throws RemoteException, ExecutionException, InterruptedException {
        CompletableFuture<Integer> futureResult = calculatorWebService.performAsyncAddOperation(1, 2);
        return futureResult.get(); // Blocking call, futureResult'ın tamamlanmasını bekler

    }

    @GetMapping("/sync/test")
    public int syncSum() throws Exception {
        return calculatorWebService.performSyncAddOperation(1, 2);
    }

    @GetMapping("/async/vt/test")
    public Integer asyncVirtualThreadSum() throws RemoteException, ExecutionException, InterruptedException {
        CompletableFuture<Integer> futureResult = calculatorWebService.performAsyncVirtualThreadAddOperation(1, 2);
        return futureResult.get(); // Blocking call, futureResult'ın tamamlanmasını bekler

    }

    @GetMapping("/async/axis/test")
    public int asyncAxisTest() throws Exception {
        CalculatorWebServiceImpl.TestCallback axisCallback = new CalculatorWebServiceImpl.TestCallback("CallBack1");
        calculatorWebService.performAxisAsync(1, 2, axisCallback);
        System.out.println("STATUS:" + axisCallback.complete);
        if (axisCallback.complete) {
            System.out.println("AxisCallBackResult: " + axisCallback.result);
        }


        // Asenkron çağrının tamamlanmasını beklemek için bir süre verilebilir
        // Örneğin, 10 saniye beklemek için:

//        while ( ! axisCallback.isComplete( ) ) {
//            Thread.sleep(100);
//        }
//
//     return axisCallback.result;
        return 2;
    }

    @GetMapping("/async/axis2/test")
    public CompletableFuture<Integer> asyncAxis2Test() {
        // CompletableFuture nesnesi oluşturuyoruz, sonucun geleceğini belirtiyoruz.
        CompletableFuture<Integer> futureResult = new CompletableFuture<>();

        try {
            // Asenkron çağrı için callback oluşturuyoruz
            CalculatorWebServiceImpl.TestCallback axisCallback = new CalculatorWebServiceImpl.TestCallback("CallBack1");

            // Servis çağrısını gerçekleştiriyoruz, sonucu axisCallback üzerinden alacağız
            calculatorWebService.performAxisAsync(1, 2, axisCallback);

            // Asenkron işlemi takip etmek için bir arka plan görevi başlatıyoruz
            Executors.newCachedThreadPool().submit(() -> {
                while (!axisCallback.isComplete()) {
                    try {
                        // İşlem tamamlanana kadar bekleyelim
                        Thread.sleep(1000); // 1 saniye bekleyelim
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                // İşlem tamamlandığında, futureResult'a sonucu ekleyelim
                futureResult.complete(axisCallback.result);
            });

        } catch (Exception e) {
            // Hata oluşursa CompletableFuture ile hata durumunu bildirelim
            futureResult.completeExceptionally(e);
        }

        return futureResult;
    }

    @GetMapping("/async/axis2/vt/test")
    public CompletableFuture<Integer> asyncVTAxis2Test() {

        CompletableFuture<Integer> futureResult = new CompletableFuture<>();

        try {
            // Sanal thread'ler için executor oluştur
            ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

            // Servis çağrısı için callback oluştur
            CalculatorWebServiceImpl.TestCallback axisCallback = new CalculatorWebServiceImpl.TestCallback("CallBack1");

            // Servis çağrısını sanal thread üzerinde gerçekleştir
            executorService.submit(() -> {
                try {
                    calculatorWebService.performAxisAsync(1, 2, axisCallback);

                    // İşlem tamamlanana kadar bekle
                    while (!axisCallback.isComplete()) {
                        Thread.sleep(1000); // 1 saniye bekleyelim
                    }

                    // İşlem tamamlandığında sonucu CompletableFuture'a ekleyelim
                    futureResult.complete(axisCallback.result);

                } catch (Exception e) {
                    // Hata durumunda CompletableFuture'a istisna ekleyelim
                    futureResult.completeExceptionally(e);
                }
            });

            // Executor'ı kapatmadan önce CompletableFuture'ı döndür
            executorService.shutdown();

        } catch (Exception e) {
            // Hata oluşursa CompletableFuture ile hata durumunu bildir
            futureResult.completeExceptionally(e);
        }

        return futureResult;
    }
}
