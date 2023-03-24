package com.anter.walletTask

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.anter.walletTask.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.EthFilter
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.EthLog
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Convert
import java.math.BigInteger

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val web3 = Web3j.build(HttpService("https://rpc.ankr.com/eth"))
    private val address = "0x7DBB4bdCfE614398D1a68ecc219F15280d0959E0"
    private val contractAddress = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48" // the address of the ERC-20 contract


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.contentMain.last50Content.movementMethod = ScrollingMovementMethod()

        // Part A: detect and display the latest 50 ERC-20 incoming token transfers (e.g. USDC token) on the Ethereum network sent to the address below.
        lifecycleScope.launch(Dispatchers.IO) {


            val transferEvent = org.web3j.abi.EventEncoder.encode(
                org.web3j.abi.datatypes.Event(
                    "Transfer",
                    listOf(
                        TypeReference.makeTypeReference("address"),
                        TypeReference.makeTypeReference("address"),
                        TypeReference.makeTypeReference("uint256")
                    )
                )
            )
            val eventFilter = EthFilter(
                DefaultBlockParameterName.LATEST,
                DefaultBlockParameterName.LATEST,
                contractAddress
            ).addSingleTopic(transferEvent)
            val transferEvents = web3.ethGetLogs(eventFilter).send().logs
            transferEvents?.let {
                for (event in transferEvents) {
                    event as EthLog.LogObject
                    val from = event.topics[1].substring(26)
                    val to = event.topics[2].substring(26)
                    val value = event.data.substring(2).toBigInteger(16)
                    Log.d("TokenTransfer", "From: $from, To: $to, Value: $value")

                    withContext(Dispatchers.Main) {
                        binding.contentMain.last50Content.append(
                            "From: $from,\nTo: $to,\nValue: $value\n\n"
                        )
                    }
                }
            }

        }

        // Part B: determine and display the current ETH balance on the Ethereum network of the address below.
        lifecycleScope.launch(Dispatchers.IO) {
            val ethGetBalance = web3.ethGetBalance(address, DefaultBlockParameterName.LATEST).send()
            val balanceInWei = ethGetBalance.balance
            val balanceInEth = Convert.fromWei(balanceInWei.toString(), Convert.Unit.ETHER)

            // display the balance in the UI
            Log.d("data_result", balanceInEth.toString())
            binding.contentMain.currentBalance.text = balanceInEth.toString()
        }

        // Part C: determine and display the current token balance of ERC-20 token (e.g. USDC token) on the Ethereum network of the that address.
        lifecycleScope.launch(Dispatchers.IO) {

            // Create the ERC-20 balanceOf function
            val function = org.web3j.abi.datatypes.Function(
                "balanceOf",
                listOf(Address(address)),
                listOf(object : TypeReference<Uint256>() {})
            )

            // Encode the function call into hexadecimal format
            val encodedFunction = FunctionEncoder.encode(function)

            // Call the contract with the encoded function and retrieve the response
            val response = web3.ethCall(
                Transaction.createEthCallTransaction(
                    null, // Set the from address to null
                    contractAddress,
                    encodedFunction
                ), DefaultBlockParameterName.LATEST
            ).send()

            // Parse the response to obtain the balance as a BigInteger
            val balance = BigInteger(response.value.substring(2), 16)

            // Convert the balance to a human-readable format
            val balanceInEther = Convert.fromWei(balance.toString(), Convert.Unit.ETHER)


            // display the balance in the UI
            Log.d("data_result", balanceInEther.toString())
            binding.contentMain.currentTokenBalance.text = balanceInEther.toString()
        }
    }
}