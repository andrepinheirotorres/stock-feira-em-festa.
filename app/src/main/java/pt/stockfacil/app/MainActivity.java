package pt.stockfacil.app;

import android.app.*;
import android.os.Bundle;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import androidx.activity.result.ActivityResultLauncher;
import java.util.*;

public class MainActivity extends androidx.activity.ComponentActivity {
    private StockDb db; private LinearLayout root; private String scanMode="";
    private final ActivityResultLauncher<ScanOptions> scanner = registerForActivityResult(new ScanContract(), result -> {
        if(result.getContents()!=null) handleCode(result.getContents().trim());
        else toast("Leitura cancelada");
    });
    @Override public void onCreate(Bundle b) { super.onCreate(b); db=new StockDb(this); home(); }
    private TextView text(String s, int size) { TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(Color.rgb(25,40,45)); t.setPadding(10,14,10,14); return t; }
    private void screen(String title) {
        ScrollView scroll=new ScrollView(this); root=new LinearLayout(this); root.setOrientation(1); root.setPadding(22,25,22,35);
        root.setBackgroundColor(Color.rgb(246,249,249)); scroll.addView(root); setContentView(scroll);
        TextView heading=text(title,27); heading.setTextColor(Color.rgb(16,105,80)); root.addView(heading);
    }
    private void button(String label, Runnable click) {
        Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(17); b.setOnClickListener(v->click.run());
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,6,0,6); root.addView(b,lp);
    }
    private EditText field(String hint, String initial, boolean number) {
        EditText e=new EditText(this); e.setHint(hint); e.setText(initial); if(number) e.setInputType(2);
        root.addView(e,new LinearLayout.LayoutParams(-1,-2)); return e;
    }
    private void toast(String s) { Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }
    private void scan(String mode) {
        scanMode=mode; ScanOptions options=new ScanOptions(); options.setPrompt("Aponte a câmara ao código de barras");
        options.setBeepEnabled(true); options.setOrientationLocked(false); scanner.launch(options);
    }
    private void home() {
        screen("StockFácil");
        int count=0, low=0; for(StockDb.Product p:db.products()){count++; if(p.qty<=p.min)low++;}
        root.addView(text("Produtos: "+count+"     Stock baixo: "+low,17));
        button("＋ ENTRADA — ler código",()->scan("in"));
        button("− SAÍDA — retirar 1 unidade",()->scan("out"));
        button("Registar produto",()->scan("new"));
        button("Consultar produtos",this::products);
        button("Histórico de movimentos",this::history);
        button("Introduzir código manualmente",()->manual("in"));
        root.addView(text("Funciona sem Internet. Os dados ficam neste telemóvel.",14));
    }
    private void manual(String mode) {
        final EditText input=new EditText(this); input.setHint("Código de barras");
        new AlertDialog.Builder(this).setTitle("Código manual").setView(input).setNegativeButton("Cancelar",null)
          .setPositiveButton("Continuar",(d,w)->{ scanMode=mode; handleCode(input.getText().toString().trim()); }).show();
    }
    private void handleCode(String barcode) {
        if(barcode.isEmpty()) { toast("Código inválido"); return; }
        StockDb.Product p=db.get(barcode);
        if("new".equals(scanMode) || p==null) { register(barcode); return; }
        if("out".equals(scanMode)) {
            if(db.move(barcode,-1)) { toast(p.name+": -1 unidade"); home(); }
            else new AlertDialog.Builder(this).setTitle("Stock esgotado").setMessage("Não é possível retirar unidades de "+p.name).setPositiveButton("OK",null).show();
        } else entry(p);
    }
    private void register(String barcode) {
        screen("Registar produto"); root.addView(text("Código: "+barcode,16));
        StockDb.Product existing=db.get(barcode);
        EditText name=field("Nome do produto",existing==null?"":existing.name,false);
        EditText min=field("Aviso de stock baixo (unidades)",existing==null?"5":String.valueOf(existing.min),true);
        button("Guardar produto",()->{
            String n=name.getText().toString().trim(); if(n.isEmpty()){toast("Introduza o nome");return;}
            try { int m=Integer.parseInt(min.getText().toString()); if(m<0)throw new Exception();
                db.saveProduct(barcode,n,m); toast("Produto guardado"); home();
            }catch(Exception ex){toast("Quantidade mínima inválida");}
        }); button("Voltar",this::home);
    }
    private void entry(StockDb.Product p) {
        screen("Entrada de stock"); root.addView(text(p.name+"\nCódigo: "+p.barcode+"\nStock atual: "+p.qty,19));
        EditText qty=field("Quantidade a adicionar","1",true);
        button("Confirmar entrada",()->{
            try {int n=Integer.parseInt(qty.getText().toString()); if(n<=0)throw new Exception();
                if(!db.move(p.barcode,n)){toast("Não foi possível adicionar stock");return;}
                toast("Adicionadas "+n+" unidades"); home();
            }catch(Exception ex){toast("Introduza uma quantidade positiva válida");}
        }); button("Cancelar",this::home);
    }
    private void products() {
        screen("Produtos"); button("Voltar",this::home);
        for(StockDb.Product p:db.products()) {
            TextView t=text(p.name+"\n"+p.barcode+"  |  "+p.qty+" unidades"+(p.qty<=p.min?"  ⚠ STOCK BAIXO":""),16);
            root.addView(t); t.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(p.name)
              .setItems(new String[]{"Adicionar unidades","Retirar 1 unidade","Editar produto"},(d,which)->{
                  if(which==0)entry(p);
                  else if(which==1){if(db.move(p.barcode,-1)){toast("-1 unidade");products();}else toast("Stock esgotado");}
                  else register(p.barcode);
              }).show());
        }
    }
    private void history() {
        screen("Histórico de movimentos"); button("Voltar",this::home);
        ArrayList<String> lines=db.history(); if(lines.isEmpty())root.addView(text("Ainda não existem movimentos.",16));
        for(String s:lines)root.addView(text(s,15));
    }
    @Override public void onBackPressed() { home(); }
}
