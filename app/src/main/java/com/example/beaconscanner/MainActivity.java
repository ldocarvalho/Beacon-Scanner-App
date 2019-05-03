package com.example.beaconscanner;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ACESS_COARSE_LOCATION = 1;
    private static final int REQUEST_ENABLE_BLUETOOTH = 11;

    private ListView listView;
    private Button button;

    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        listView = findViewById(R.id.listView);
        button = findViewById(R.id.button);

        // pegar o adaptador padrao
        listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        listView.setAdapter(listAdapter);

        // checar o estado do bluetooth
        //checaEstadoBluetooth();

        // cria registros de receptores pra rodar na activity
        // caso um dispositivo seja encontrado
        registerReceiver(receptorDispositivos, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        // caso a busca se inicie
        registerReceiver(receptorDispositivos, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
        // caso a busca finalize
        registerReceiver(receptorDispositivos, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // se o adaptador nao for nulo e Bluetooth estiver ativado
                if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()){
                    // checa se a localizacao ta permitida e inicia a busca de dispositivos
                    if (checaPermissaoLocalizacao()){
                        listAdapter.clear();
                        bluetoothAdapter.startDiscovery();
                    }
                } else {
                    checaEstadoBluetooth();
                }
            }
        });

        checaPermissaoLocalizacao();
    }

    protected void onPause(){
        super.onPause();
        unregisterReceiver(receptorDispositivos);
    }

    protected void onStop(){
        super.onStop();
        unregisterReceiver(receptorDispositivos);
    }

    // checa a permissao pra localizacao do dispositivo
    private boolean checaPermissaoLocalizacao(){
        // se a localizacao nao estiver permitida, pede permissao pra ligar
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_ACESS_COARSE_LOCATION);
            return false;
        } else
            return true;
    }

    // checa o estado do adaptador Bluetooth e do estado do Bluetooth no dispositivo
    private void checaEstadoBluetooth(){
        // se o adaptador padrao for nulo, o dispositivo nao tem suporte pro BLE
        if (bluetoothAdapter == null){
            Toast.makeText(this, "Seu dispositivo não tem suporte ao Bluetooth.", Toast.LENGTH_LONG).show();
        } else {
            if (bluetoothAdapter.isEnabled()){
                if (bluetoothAdapter.isDiscovering()){
                    Toast.makeText(this, "Buscando novos dispositivos...", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Bluetooth não está buscando", Toast.LENGTH_SHORT).show();
                    button.setEnabled(true);
                }
            } else {
                Toast.makeText(this, "Ative o Bluetooth", Toast.LENGTH_LONG).show();
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
            }
        }
    }

    @Override
    // retoma a checagem do estado do Bluetooth
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BLUETOOTH){
            checaEstadoBluetooth();
        }
    }

    @Override
    // mostra resultado da acao de permissao pra acesso a localizacao do dispositivo
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case REQUEST_ACESS_COARSE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this, "Acesso à localização permitido.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Acesso à localização negado.", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    private final BroadcastReceiver receptorDispositivos = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String acao = intent.getAction();

            // se o adaptador encontrou um dispositivo novo
            if (BluetoothDevice.ACTION_FOUND.equals(acao)){
                // recolhe as informacoes do dispositivo mapeado
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int  rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE);
                // mostra na tela os valores lidos
                listAdapter.add("Nome: " + bluetoothDevice.getName() + "\n"
                        + "Endereco: " + bluetoothDevice.getAddress() + "\n"
                        + "RSSI: " + rssi + " dB");
                listAdapter.notifyDataSetChanged();
            } else if (bluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(acao)){
                // se a busca foi finalizada pelo adaptador, muda o texto do botao
                button.setText("Leitura finalizada");
            } else if (bluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(acao)){
                // se a busca foi iniciada pelo adaptador, muda o texto do botao
                button.setText("Buscando dispositivos...");
            }
        }
    };
}