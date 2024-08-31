package fr.mossaab.security.service.impl;

import fr.mossaab.security.entities.Barcode;
import fr.mossaab.security.entities.BarcodeType;
import fr.mossaab.security.repository.BarcodeRepository;
import fr.mossaab.security.repository.BarcodeTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BarcodeService {

    @Autowired
    private BarcodeRepository barcodeRepository;

    @Autowired
    private BarcodeTypeRepository barcodeTypeRepository;

    @Transactional
    public Barcode addBarcode(Long code, Long barcodeTypeId) {
        // Проверка, что штрихкод с таким кодом уже не существует
        if (barcodeRepository.existsByCode(code)) {
            throw new RuntimeException("Barcode with code " + code + " already exists.");
        }

        BarcodeType barcodeType = barcodeTypeRepository.findById(barcodeTypeId)
                .orElseThrow(() -> new RuntimeException("BarcodeType not found with id: " + barcodeTypeId));

        Barcode barcode = new Barcode();
        barcode.setCode(code);
        barcode.setUsed(false);
        barcode.setBarcodeType(barcodeType);

        return barcodeRepository.save(barcode);
    }

    @Transactional
    public Barcode markBarcodeAsUsed(Long code) {
        Barcode barcode = barcodeRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Barcode not found with code: " + code));

        if (barcode.isUsed()) {
            throw new RuntimeException("Barcode is already used.");
        }

        barcode.setUsed(true);
        return barcodeRepository.save(barcode);
    }
}
