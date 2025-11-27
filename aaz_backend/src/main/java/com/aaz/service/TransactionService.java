package com.aaz.service;

import com.aaz.dto.*;
import com.aaz.entity.*;
import com.aaz.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

        private final ExpenseRepository expenseRepository;
        private final IncomeRepository incomeRepository;
        private final TransferRepository transferRepository;
        private final MemberRepository memberRepository;
        private final TripGroupRepository tripGroupRepository;
        private final ExchangeRateService exchangeRateService;

        @Transactional
        public TransactionResponse createExpense(Long tripId, ExpenseRequest request) {
                TripGroup tripGroup = tripGroupRepository.findById(tripId)
                                .orElseThrow(() -> new RuntimeException("Trip group not found"));

                // 驗證付款總額
                BigDecimal totalPaid = request.getPayments().stream()
                                .map(PaymentRequest::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (totalPaid.compareTo(request.getAmount()) != 0) {
                        throw new RuntimeException("Total payment amount must equal expense amount");
                }

                // 驗證分攤總額
                BigDecimal totalSplit = request.getSplits().stream()
                                .map(SplitRequest::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (totalSplit.compareTo(request.getAmount()) != 0) {
                        throw new RuntimeException("Total split amount must equal expense amount");
                }

                // 建立支出
                Expense expense = new Expense();
                expense.setTripGroup(tripGroup);
                expense.setTitle(request.getTitle());
                expense.setCategory(request.getCategory());
                expense.setAmount(request.getAmount());
                expense.setCurrency(request.getCurrency());
                expense.setExpenseDate(request.getExpenseDate());
                expense.setNotes(request.getNotes());

                // 處理匯率
                if (request.getExchangeRate() != null) {
                        expense.setExchangeRate(request.getExchangeRate());
                } else if (!request.getCurrency().equals(tripGroup.getBaseCurrency())) {
                        BigDecimal rate = exchangeRateService.getRate(
                                        request.getCurrency(),
                                        tripGroup.getBaseCurrency(),
                                        request.getExpenseDate());
                        expense.setExchangeRate(rate);
                }

                // 計算換算後金額
                if (expense.getExchangeRate() != null) {
                        expense.setConvertedAmount(request.getAmount().multiply(expense.getExchangeRate()));
                } else {
                        expense.setConvertedAmount(request.getAmount());
                }

                expense = expenseRepository.save(expense);

                // 建立付款記錄
                for (PaymentRequest payment : request.getPayments()) {
                        Member member = memberRepository.findById(payment.getMemberId())
                                        .orElseThrow(() -> new RuntimeException("Member not found"));

                        ExpensePayment expensePayment = new ExpensePayment();
                        expensePayment.setExpense(expense);
                        expensePayment.setMember(member);
                        expensePayment.setAmount(payment.getAmount());
                        expense.getPayments().add(expensePayment);
                }

                // 建立分攤記錄
                for (SplitRequest split : request.getSplits()) {
                        Member member = memberRepository.findById(split.getMemberId())
                                        .orElseThrow(() -> new RuntimeException("Member not found"));

                        ExpenseSplit expenseSplit = new ExpenseSplit();
                        expenseSplit.setExpense(expense);
                        expenseSplit.setMember(member);
                        expenseSplit.setAmount(split.getAmount());
                        expenseSplit.setSplitType(ExpenseSplit.SplitType.valueOf(split.getSplitType()));
                        expense.getSplits().add(expenseSplit);
                }

                // 儲存匯率記錄
                if (expense.getExchangeRate() != null && !request.getCurrency().equals(tripGroup.getBaseCurrency())) {
                        exchangeRateService.saveRate(
                                        request.getCurrency(),
                                        tripGroup.getBaseCurrency(),
                                        request.getExpenseDate(),
                                        expense.getExchangeRate());
                }

                expense = expenseRepository.save(expense);
                return convertExpenseToResponse(expense);
        }

        @Transactional
        public TransactionResponse createIncome(Long tripId, IncomeRequest request) {
                TripGroup tripGroup = tripGroupRepository.findById(tripId)
                                .orElseThrow(() -> new RuntimeException("Trip group not found"));

                // 驗證收款總額
                BigDecimal totalReceived = request.getReceivers().stream()
                                .map(ReceiverRequest::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (totalReceived.compareTo(request.getAmount()) != 0) {
                        throw new RuntimeException("Total received amount must equal income amount");
                }

                // 驗證分攤總額
                BigDecimal totalSplit = request.getSplits().stream()
                                .map(SplitRequest::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (totalSplit.compareTo(request.getAmount()) != 0) {
                        throw new RuntimeException("Total split amount must equal income amount");
                }

                // 建立收入
                Income income = new Income();
                income.setTripGroup(tripGroup);
                income.setTitle(request.getTitle());
                income.setCategory(request.getCategory());
                income.setAmount(request.getAmount());
                income.setCurrency(request.getCurrency());
                income.setIncomeDate(request.getIncomeDate());
                income.setNotes(request.getNotes());

                // 處理匯率
                if (request.getExchangeRate() != null) {
                        income.setExchangeRate(request.getExchangeRate());
                } else if (!request.getCurrency().equals(tripGroup.getBaseCurrency())) {
                        BigDecimal rate = exchangeRateService.getRate(
                                        request.getCurrency(),
                                        tripGroup.getBaseCurrency(),
                                        request.getIncomeDate());
                        income.setExchangeRate(rate);
                }

                // 計算換算後金額
                if (income.getExchangeRate() != null) {
                        income.setConvertedAmount(request.getAmount().multiply(income.getExchangeRate()));
                } else {
                        income.setConvertedAmount(request.getAmount());
                }

                income = incomeRepository.save(income);

                // 建立收款記錄
                for (ReceiverRequest receiver : request.getReceivers()) {
                        Member member = memberRepository.findById(receiver.getMemberId())
                                        .orElseThrow(() -> new RuntimeException("Member not found"));

                        IncomeReceiver incomeReceiver = new IncomeReceiver();
                        incomeReceiver.setIncome(income);
                        incomeReceiver.setMember(member);
                        incomeReceiver.setAmount(receiver.getAmount());
                        income.getReceivers().add(incomeReceiver);
                }

                // 建立分攤記錄
                for (SplitRequest split : request.getSplits()) {
                        Member member = memberRepository.findById(split.getMemberId())
                                        .orElseThrow(() -> new RuntimeException("Member not found"));

                        IncomeSplit incomeSplit = new IncomeSplit();
                        incomeSplit.setIncome(income);
                        incomeSplit.setMember(member);
                        incomeSplit.setAmount(split.getAmount());
                        incomeSplit.setSplitType(IncomeSplit.SplitType.valueOf(split.getSplitType()));
                        income.getSplits().add(incomeSplit);
                }

                // 儲存匯率記錄
                if (income.getExchangeRate() != null && !request.getCurrency().equals(tripGroup.getBaseCurrency())) {
                        exchangeRateService.saveRate(
                                        request.getCurrency(),
                                        tripGroup.getBaseCurrency(),
                                        request.getIncomeDate(),
                                        income.getExchangeRate());
                }

                income = incomeRepository.save(income);
                return convertIncomeToResponse(income);
        }

        @Transactional
        public TransactionResponse createTransfer(Long tripId, TransferRequest request) {
                TripGroup tripGroup = tripGroupRepository.findById(tripId)
                                .orElseThrow(() -> new RuntimeException("Trip group not found"));

                Member fromMember = memberRepository.findById(request.getFromMemberId())
                                .orElseThrow(() -> new RuntimeException("From member not found"));

                Member toMember = memberRepository.findById(request.getToMemberId())
                                .orElseThrow(() -> new RuntimeException("To member not found"));

                if (fromMember.getId().equals(toMember.getId())) {
                        throw new RuntimeException("Cannot transfer to the same member");
                }

                // 建立轉帳
                Transfer transfer = new Transfer();
                transfer.setTripGroup(tripGroup);
                transfer.setFromMember(fromMember);
                transfer.setToMember(toMember);
                transfer.setAmount(request.getAmount());
                transfer.setCurrency(request.getCurrency());
                transfer.setTransferDate(request.getTransferDate());
                transfer.setNotes(request.getNotes());

                // 處理匯率
                if (request.getExchangeRate() != null) {
                        transfer.setExchangeRate(request.getExchangeRate());
                } else if (!request.getCurrency().equals(tripGroup.getBaseCurrency())) {
                        BigDecimal rate = exchangeRateService.getRate(
                                        request.getCurrency(),
                                        tripGroup.getBaseCurrency(),
                                        request.getTransferDate());
                        transfer.setExchangeRate(rate);
                }

                // 計算換算後金額
                if (transfer.getExchangeRate() != null) {
                        transfer.setConvertedAmount(request.getAmount().multiply(transfer.getExchangeRate()));
                } else {
                        transfer.setConvertedAmount(request.getAmount());
                }

                // 儲存匯率記錄
                if (transfer.getExchangeRate() != null && !request.getCurrency().equals(tripGroup.getBaseCurrency())) {
                        exchangeRateService.saveRate(
                                        request.getCurrency(),
                                        tripGroup.getBaseCurrency(),
                                        request.getTransferDate(),
                                        transfer.getExchangeRate());
                }

                transfer = transferRepository.save(transfer);
                return convertTransferToResponse(transfer);
        }

        public List<TransactionResponse> getAllTransactions(Long tripId) {
                List<TransactionResponse> transactions = new ArrayList<>();

                // 加入支出
                List<Expense> expenses = expenseRepository.findByTripGroupIdOrderByExpenseDateDesc(tripId);
                transactions.addAll(expenses.stream()
                                .map(this::convertExpenseToResponse)
                                .collect(Collectors.toList()));

                // 加入收入
                List<Income> incomes = incomeRepository.findByTripGroupIdOrderByIncomeDateDesc(tripId);
                transactions.addAll(incomes.stream()
                                .map(this::convertIncomeToResponse)
                                .collect(Collectors.toList()));

                // 加入轉帳
                List<Transfer> transfers = transferRepository.findByTripGroupIdOrderByTransferDateDesc(tripId);
                transactions.addAll(transfers.stream()
                                .map(this::convertTransferToResponse)
                                .collect(Collectors.toList()));

                // 按日期排序
                transactions.sort((t1, t2) -> t2.getTransactionDate().compareTo(t1.getTransactionDate()));

                return transactions;
        }

        private TransactionResponse convertExpenseToResponse(Expense expense) {
                TransactionResponse response = new TransactionResponse();
                response.setTransactionId("E" + expense.getId());
                response.setType("expense");
                response.setTitle(expense.getTitle());
                response.setCategory(expense.getCategory());
                response.setAmount(expense.getAmount());
                response.setCurrency(expense.getCurrency());
                response.setConvertedAmount(expense.getConvertedAmount());
                response.setTransactionDate(expense.getExpenseDate());
                response.setNotes(expense.getNotes());

                List<PaymentResponse> payments = expense.getPayments().stream()
                                .map(p -> new PaymentResponse(
                                                p.getMember().getId(),
                                                p.getMember().getDisplayName(),
                                                p.getAmount()))
                                .collect(Collectors.toList());
                response.setPayments(payments);

                List<SplitResponse> splits = expense.getSplits().stream()
                                .map(s -> new SplitResponse(
                                                s.getMember().getId(),
                                                s.getMember().getDisplayName(),
                                                s.getAmount(),
                                                s.getSplitType().name()))
                                .collect(Collectors.toList());
                response.setSplits(splits);

                return response;
        }

        private TransactionResponse convertIncomeToResponse(Income income) {
                TransactionResponse response = new TransactionResponse();
                response.setTransactionId("I" + income.getId());
                response.setType("income");
                response.setTitle(income.getTitle());
                response.setCategory(income.getCategory());
                response.setAmount(income.getAmount());
                response.setCurrency(income.getCurrency());
                response.setConvertedAmount(income.getConvertedAmount());
                response.setTransactionDate(income.getIncomeDate());
                response.setNotes(income.getNotes());

                List<PaymentResponse> receivers = income.getReceivers().stream()
                                .map(r -> new PaymentResponse(
                                                r.getMember().getId(),
                                                r.getMember().getDisplayName(),
                                                r.getAmount()))
                                .collect(Collectors.toList());
                response.setPayments(receivers);

                List<SplitResponse> splits = income.getSplits().stream()
                                .map(s -> new SplitResponse(
                                                s.getMember().getId(),
                                                s.getMember().getDisplayName(),
                                                s.getAmount(),
                                                s.getSplitType().name()))
                                .collect(Collectors.toList());
                response.setSplits(splits);

                return response;
        }

        private TransactionResponse convertTransferToResponse(Transfer transfer) {
                TransactionResponse response = new TransactionResponse();
                response.setTransactionId("T" + transfer.getId());
                response.setType("transfer");
                response.setTitle("轉帳: " + transfer.getFromMember().getDisplayName() +
                                " → " + transfer.getToMember().getDisplayName());
                response.setCategory("轉帳");
                response.setAmount(transfer.getAmount());
                response.setCurrency(transfer.getCurrency());
                response.setConvertedAmount(transfer.getConvertedAmount());
                response.setTransactionDate(transfer.getTransferDate());
                response.setNotes(transfer.getNotes());

                // 轉帳記錄付款人和收款人
                List<PaymentResponse> payments = Arrays.asList(
                                new PaymentResponse(
                                                transfer.getFromMember().getId(),
                                                transfer.getFromMember().getDisplayName(),
                                                transfer.getAmount()));
                response.setPayments(payments);

                List<SplitResponse> splits = Arrays.asList(
                                new SplitResponse(
                                                transfer.getToMember().getId(),
                                                transfer.getToMember().getDisplayName(),
                                                transfer.getAmount(),
                                                "TRANSFER"));
                response.setSplits(splits);

                return response;
        }
}