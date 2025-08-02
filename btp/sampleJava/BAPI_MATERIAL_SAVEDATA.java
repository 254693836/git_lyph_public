import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoParameterList;
import com.sap.conn.jco.JCoTable;

public class MaterialMasterUpdate {

    public static void main(String[] args) {
        try {
            // SAP接続先を取得（事前にdestination設定済み）
            JCoDestination destination = JCoDestinationManager.getDestination("MY_S4_DEST");

            // BAPI_MATERIAL_SAVEDATAの関数を取得
            JCoFunction bapiMaterialSave = destination.getRepository().getFunction("BAPI_MATERIAL_SAVEDATA");
            if (bapiMaterialSave == null) {
                throw new RuntimeException("BAPI_MATERIAL_SAVEDATA function not found in SAP.");
            }

            // HEADDATA構造体（品目基本情報）の設定
            JCoParameterList importParamList = bapiMaterialSave.getImportParameterList();
            importParamList.getStructure("HEADDATA").setValue("MATERIAL", "MAT10001");  // 品目番号（既存品目の更新例）
            importParamList.getStructure("HEADDATA").setValue("IND_SECTOR", "M");      // 業種（製造業）
            importParamList.getStructure("HEADDATA").setValue("MATL_TYPE", "FERT");    // 品目タイプ（製品）

            // STORAGELOCATIONDATAテーブルの設定（例：工場1000の保管場所0001を登録）
            JCoTable storageLocationTable = bapiMaterialSave.getTableParameterList().getTable("STORAGELOCATIONDATA");
            storageLocationTable.appendRow();
            storageLocationTable.setValue("PLANT", "1000");
            storageLocationTable.setValue("STGE_LOC", "0001");
            storageLocationTable.setValue("SLED_DAT", "20250801");  // 消費期限日（例）

            // SALESDATAテーブルの設定（例：販売組織1000、流通チャネル10、部門00）
            JCoTable salesDataTable = bapiMaterialSave.getTableParameterList().getTable("SALESDATA");
            salesDataTable.appendRow();
            salesDataTable.setValue("SALES_ORG", "1000");
            salesDataTable.setValue("DIST_CHANNEL", "10");
            salesDataTable.setValue("DIVISION", "00");

            // PLANTDATAテーブルの設定（例：工場1000のMRPタイプPD、仕入区分F）
            JCoTable plantDataTable = bapiMaterialSave.getTableParameterList().getTable("PLANTDATA");
            plantDataTable.appendRow();
            plantDataTable.setValue("PLANT", "1000");
            plantDataTable.setValue("MRP_TYPE", "PD");
            plantDataTable.setValue("BESKZ", "F");

            // BAPIを実行
            bapiMaterialSave.execute(destination);

            // RETURNテーブルで結果を確認
            JCoTable returnTable = bapiMaterialSave.getTableParameterList().getTable("RETURN");
            boolean hasError = false;

            for (int i = 0; i < returnTable.getNumRows(); i++) {
                returnTable.setRow(i);
                String type = returnTable.getString("TYPE");        // メッセージタイプ（S,W,E,A,I）
                String message = returnTable.getString("MESSAGE");  // メッセージ内容
                String id = returnTable.getString("ID");            // メッセージID
                String number = returnTable.getString("NUMBER");    // メッセージ番号

                System.out.println(String.format("Type:%s, ID:%s, No:%s, Message:%s", type, id, number, message));

                // エラー（E）やアボート（A）があればエラー判定
                if ("E".equalsIgnoreCase(type) || "A".equalsIgnoreCase(type)) {
                    hasError = true;
                }
            }

            if (hasError) {
                System.err.println("エラーが発生したため、トランザクションをコミットせず処理を中断します。");
                return;  // エラーなら処理終了（コミットしない）
            }

            // エラーなしならコミット呼び出し
            JCoFunction commit = destination.getRepository().getFunction("BAPI_TRANSACTION_COMMIT");
            commit.execute(destination);

            System.out.println("品目マスタの登録・更新が正常に完了しました。");

        } catch (JCoException e) {
            e.printStackTrace();
        }
    }
}
