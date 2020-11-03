import java.security.PublicKey;
import java.util.HashSet;
import java.util.Set;

public class TxHandler {
    UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
        double sumInput = 0.0;
        double sumOutput = 0.0;
        UTXOPool uniqueUtxos = new UTXOPool();

        for (int i = 0; i < tx.numOutputs(); i++) {
            // (4) non-negative
            if (tx.getOutput(i).value < 0) return false;

            // (5) comparison
            sumOutput += tx.getOutput(i).value;
        };

        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input input = tx.getInput(i);
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            // (1) tx is in UTXO pool
            if (!utxoPool.contains(utxo)) return false;

            // (2) valid signature
            Transaction.Output output = utxoPool.getTxOutput(utxo);
            PublicKey pubkey = output.address;
            byte[] message = tx.getRawDataToSign(i);
            byte[] signature = input.signature;
            if (!Crypto.verifySignature(pubkey, message, signature)) return false;

            // (3) non repetitive
            if (uniqueUtxos.contains(utxo)) return false;
            uniqueUtxos.addUTXO(utxo, output);

            // (5) comparison
            sumInput += output.value;
        }
        // (5) comparison
        return sumInput >= sumOutput;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        Set<Transaction> validTXList = new HashSet<>();

        for (int i=0; i<possibleTxs.length; i++) {

            if (isValidTx(possibleTxs[i])) {
                // check correctness
                validTXList.add(possibleTxs[i]);
                // update UXTO
                for (Transaction.Input in : possibleTxs[i].getInputs()) {
                    UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
                    utxoPool.removeUTXO(utxo);
                }
                for (int j = 0; j < possibleTxs[i].numOutputs(); j++) {
                    Transaction.Output out = possibleTxs[i].getOutput(j);
                    UTXO utxo = new UTXO(possibleTxs[i].getHash(), j);
                    utxoPool.addUTXO(utxo, out);
                }
            }
        }
        Transaction[] validTxArray = new Transaction[validTXList.size()];
        return validTXList.toArray(validTxArray);
    }

}
